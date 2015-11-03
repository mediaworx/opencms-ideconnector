package com.mediaworx.opencms.ideconnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.opencms.file.*;
import org.opencms.i18n.CmsEncoder;
import org.opencms.importexport.CmsExport;
import org.opencms.importexport.CmsImportExportManager;
import org.opencms.importexport.CmsImportVersion7;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.module.CmsModule;
import org.opencms.module.CmsModuleXmlHandler;
import org.opencms.relations.CmsRelation;
import org.opencms.relations.CmsRelationFilter;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.security.CmsRole;
import org.opencms.util.CmsDateUtil;
import org.opencms.util.CmsUUID;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

/**
 * Helper Class providing methods to generate the module manifest stub or resource infos.
 *
 * @author Kai Widman, 2013/2014 mediaworx Berlin AG
 */
public class MetaXmlHelper extends CmsExport {

	private static final Log LOG = LogFactory.getLog(MetaXmlHelper.class);
	private static final String NODE_FILE_INFO = "fileinfo";
	private static final String NODE_SIBLING_COUNT = "siblingcount";

	private CmsObject cmsObject;
	private boolean useDateVariablesEnabled = false;
	private boolean useIdVariablesEnabled = false;

	/**
	 * Creates a new MetaXmlHelper that uses the given CmsObject to read meta information for modules or resources from
	 * OpenCms.
	 * @param cmsObject the CmsObject to be used to read meta information from OpenCms
	 */
	public MetaXmlHelper(CmsObject cmsObject) {
		this.cmsObject = cmsObject;
	}

	/**
	 * sets the flag useDateVariablesEnabled. See {@link OpenCmsIDEConnector} for an explanation what
	 * <code>useDateVariables</code> does.
	 * @param useMetaVariables <code>true</code> if date meta data (modified date and created date) should be replaced
	 *                         by placeholders), <code>false</code> otherwise
	 *
	 */
	public void setUseDateVariables(boolean useMetaVariables) {
		this.useDateVariablesEnabled = useMetaVariables;
	}

	/**
	 * sets the flag useIdVariablesEnabled. See {@link OpenCmsIDEConnector} for an explanation what
	 * <code>useIdVariables</code> does.
	 *
	 * @param useIdVariables <code>true</code> if ID meta data (resource UUID and structure UUID) should be replaced by
	 *                       placeholders), <code>false</code> otherwise
	 */
	public void setUseIdVariables(boolean useIdVariables) {
		this.useIdVariablesEnabled = useIdVariables;
	}

	/**
	 * Creates the module manifest stub XML (OpenCms module manifest with an empty files node) for the given module.
	 * <br /><br />
	 * Sample syntax:
	 * <pre>
	 * &lt;export&gt;
	 *     &lt;info&gt;
	 *         &lt;creator&gt;Admin&lt;/creator&gt;
	 *         &lt;opencms_version&gt;8.5.1&lt;/opencms_version&gt;
	 *         &lt;createdate&gt;Fri, 17 Jan 2014 15:35:11 GMT&lt;/createdate&gt;
	 *         &lt;infoproject&gt;Offline&lt;/infoproject&gt;
	 *         &lt;export_version&gt;7&lt;/export_version&gt;
	 *     &lt;/info&gt;
	 *     &lt;module&gt;
	 *         &lt;name&gt;com.mycompany.mypackage.mymodule1&lt;/name&gt;
	 *         &lt;nicename&gt;&lt;![CDATA[My Module 1]]&gt;&lt;/nicename&gt;
	 *         &lt;class/&gt;
	 *         &lt;description/&gt;
	 *         &lt;version&gt;0.4&lt;/version&gt;
	 *         &lt;authorname&gt;&lt;![CDATA[My Name]]&gt;&lt;/authorname&gt;
	 *         &lt;authoremail&gt;&lt;![CDATA[my_mail@some-host.org]]&gt;&lt;/authoremail&gt;
	 *         &lt;datecreated/&gt;
	 *         &lt;userinstalled/&gt;
	 *         &lt;dateinstalled/&gt;
	 *         &lt;dependencies/&gt;
	 *         &lt;exportpoints&gt;
	 *             &lt;exportpoint uri="/system/modules/com.mycompany.mypackage.mymodule1/classes/" destination="WEB-INF/classes/"/&gt;
	 *         &lt;/exportpoints&gt;
	 *         &lt;resources&gt;
	 *             &lt;resource uri="/system/modules/com.mycompany.mypackage.mymodule1/"/&gt;
	 *         &lt;/resources&gt;
	 *         &lt;parameters/&gt;
	 *     &lt;/module&gt;
	 *     &lt;files/&gt;
	 * &lt;/export&gt;
	 * </pre>
	 * @param moduleName    name of the module for which the manifest stub should be returned
	 *                      (e.g. "com.mycompany.mypackage.mymodule1")
	 * @return  the manifest stub XML for the given module.
	 * @throws IllegalArgumentException
	 */
	public String getModuleManifestStub(String moduleName) throws IllegalArgumentException {
		Element exportElement = DocumentHelper.createElement(CmsImportExportManager.N_EXPORT);
		exportElement.add(getExportInfoElement());
		exportElement.add(getModuleElement(moduleName));
		exportElement.addElement(CmsImportVersion7.N_FILES);
		return getFormattedStringForDocument(DocumentHelper.createDocument(exportElement));
	}

	/**
	 * Creates the resource info XML for the VFS file/folder at the given resource path.
	 * <br /><br />
	 * Sample xml for VFS folders:
	 * <pre>
	 * &lt;file&gt;
	 *     &lt;destination&gt;testfolder&lt;/destination&gt;
	 *     &lt;type&gt;folder&lt;/type&gt;
	 *     &lt;uuidstructure&gt;41a22af8-4b8f-11e3-b543-210cc9a3bba6&lt;/uuidstructure&gt;
	 *     &lt;datelastmodified&gt;Tue, 12 Nov 2013 11:40:40 GMT&lt;/datelastmodified&gt;
	 *     &lt;userlastmodified&gt;Admin&lt;/userlastmodified&gt;
	 *     &lt;datecreated&gt;Tue, 12 Nov 2013 11:40:40 GMT&lt;/datecreated&gt;
	 *     &lt;usercreated&gt;Admin&lt;/usercreated&gt;
	 *     &lt;flags&gt;0&lt;/flags&gt;
	 *     &lt;properties/&gt;
	 *     &lt;relations/&gt;
	 *     &lt;accesscontrol/&gt;
	 * &lt;/file&gt;
	 * </pre>
	 * Sample xml for VFS files:
	 * <pre>
	 * &lt;fileinfo&gt;
	 *     &lt;file&gt;
	 *         &lt;source&gt;testfolder/testfile.jsp&lt;/source&gt;
	 *         &lt;destination&gt;testfolder/testfile.jsp&lt;/destination&gt;
	 *         &lt;type&gt;plain&lt;/type&gt;
	 *         &lt;uuidstructure&gt;0e436b9f-5c5d-11e3-91b4-210cc9a3bba6&lt;/uuidstructure&gt;
	 *         &lt;uuidresource&gt;0e436ba0-5c5d-11e3-91b4-210cc9a3bba6&lt;/uuidresource&gt;
	 *         &lt;datelastmodified&gt;Thu, 05 Dec 2013 23:31:52 GMT&lt;/datelastmodified&gt;
	 *         &lt;userlastmodified&gt;Admin&lt;/userlastmodified&gt;
	 *         &lt;datecreated&gt;Tue, 03 Dec 2013 20:54:09 GMT&lt;/datecreated&gt;
	 *         &lt;usercreated&gt;Admin&lt;/usercreated&gt;
	 *         &lt;flags&gt;0&lt;/flags&gt;
	 *         &lt;properties/&gt;
	 *         &lt;relations/&gt;
	 *         &lt;accesscontrol/&gt;
	 *     &lt;/file&gt;
	 *     &lt;siblingcount&gt;1&lt;/siblingcount&gt;
	 * &lt;/fileinfo&gt;
	 * </pre>
	 * @param resourcePath  VFS path of the resource for which meta information should be returned.
	 * @return meta info XML for the resource at the given path.
	 */
	public String getResourceInfo(String resourcePath) {
		try {
			CmsResource resource = cmsObject.readResource(resourcePath);
			if (!resource.isFolder()) {
				Element resourceInfo = DocumentHelper.createElement(NODE_FILE_INFO);
				resourceInfo.add(getFileElement(resource));
				Element siblingCount = resourceInfo.addElement(NODE_SIBLING_COUNT);
				siblingCount.setText(String.valueOf(resource.getSiblingCount()));
				return getFormattedStringForDocument(DocumentHelper.createDocument(resourceInfo));
			}
			else {
				return getFormattedStringForDocument(DocumentHelper.createDocument(getFileElement(resource)));
			}
		}
		catch (CmsException e) {
			LOG.error("Resource " + resourcePath + " can't be read", e);
			return null;
		}
	}

	/**
	 * Internal method to generate the <code>export/info</code> node of the module manifest stub.
	 * @return  the <code>export/info</code> node.
	 */
	private Element getExportInfoElement() {
		Element info = DocumentHelper.createElement(CmsImportExportManager.N_INFO);
		info.addElement(CmsImportExportManager.N_CREATOR).addText(OpenCms.getDefaultUsers().getUserAdmin());
		info.addElement(CmsImportExportManager.N_OC_VERSION).addText(OpenCms.getSystemInfo().getVersionNumber());
		String headerDate = useDateVariablesEnabled ? "${" + CmsImportExportManager.N_DATE + "}" : CmsDateUtil.getHeaderDate(System.currentTimeMillis());
		info.addElement(CmsImportExportManager.N_DATE).addText(headerDate);
		info.addElement(CmsImportExportManager.N_INFO_PROJECT).addText("Offline");
		info.addElement(CmsImportExportManager.N_VERSION).addText(CmsImportExportManager.EXPORT_VERSION);
		return info;
	}

	/**
	 * Internal method to generate the <code>export/module</code> node of the module manifest stub.
	 * Uses the standard OpenCms method <code>CmsModuleXmlHandler.generateXml(module)</code> to generate the XML
	 * element.
	 * @param  moduleName name of the module for which the <code>export/module</code> node should be returned
	 * @return  the <code>export/module</code> node
	 */
	private Element getModuleElement(String moduleName) throws IllegalArgumentException {
		CmsModule module = OpenCms.getModuleManager().getModule(moduleName);
		if (module == null) {
			throw new IllegalArgumentException(moduleName + " is not a valid OpenCms module");
		}
		return CmsModuleXmlHandler.generateXml(module);
	}

	/**
	 * Internal method used to create the XML file node for the given resource. This is a modified version of the
	 * standard OpenCms method <code>org.opencms.importexport.CmsExport.appendResourceToManifest</code>.
	 * @param resource  the resource for which the file node shold be returned.
	 * @return  the file node for the given resource.
	 */
	private Element getFileElement(CmsResource resource) {

		String rootPath = resource.getRootPath();

		try {
			String fileName = trimResourceName(rootPath);

			// it is not allowed to export organizational unit resources
			if (fileName.startsWith("system/orgunits")) {
				return null;
			}

			// define the file node
			Element fileElement = DocumentHelper.createElement(CmsImportVersion7.N_FILE);

			// only write <source> if resource is a file
			if (resource.isFile()) {
				fileElement.addElement(CmsImportVersion7.N_SOURCE).addText("${" + CmsImportVersion7.N_SOURCE + "}");
			}
			fileElement.addElement(CmsImportVersion7.N_DESTINATION).addText("${" + CmsImportVersion7.N_DESTINATION + "}");
			fileElement.addElement(CmsImportVersion7.N_TYPE).addText(OpenCms.getResourceManager().getResourceType(resource.getTypeId()).getTypeName());

			fileElement.addElement(CmsImportVersion7.N_UUIDSTRUCTURE).addText(useIdVariablesEnabled ? "${" + CmsImportVersion7.N_UUIDSTRUCTURE + "}" : resource.getStructureId().toString());
			if (resource.isFile()) {
				fileElement.addElement(CmsImportVersion7.N_UUIDRESOURCE).addText(useDateVariablesEnabled ? "${" + CmsImportVersion7.N_UUIDRESOURCE + "}" : resource.getResourceId().toString());
			}
			fileElement.addElement(CmsImportVersion7.N_DATELASTMODIFIED).addText(useDateVariablesEnabled ? "${" + CmsImportVersion7.N_DATELASTMODIFIED + "}" : CmsDateUtil.getHeaderDate(resource.getDateLastModified()));
			String userNameLastModified;
			try {
				userNameLastModified = cmsObject.readUser(resource.getUserLastModified()).getName();
			}
			catch (CmsException e) {
				userNameLastModified = OpenCms.getDefaultUsers().getUserAdmin();
			}
			fileElement.addElement(CmsImportVersion7.N_USERLASTMODIFIED).addText(userNameLastModified);
			fileElement.addElement(CmsImportVersion7.N_DATECREATED).addText(useIdVariablesEnabled ? "${" + CmsImportVersion7.N_DATECREATED + "}" : CmsDateUtil.getHeaderDate(resource.getDateCreated()));
			String userNameCreated;
			try {
				userNameCreated = cmsObject.readUser(resource.getUserCreated()).getName();
			}
			catch (CmsException e) {
				userNameCreated = OpenCms.getDefaultUsers().getUserAdmin();
			}
			fileElement.addElement(CmsImportVersion7.N_USERCREATED).addText(userNameCreated);
			if (resource.getDateReleased() != CmsResource.DATE_RELEASED_DEFAULT) {
				fileElement.addElement(CmsImportVersion7.N_DATERELEASED).addText(CmsDateUtil.getHeaderDate(resource.getDateReleased()));
			}
			if (resource.getDateExpired() != CmsResource.DATE_EXPIRED_DEFAULT) {
				fileElement.addElement(CmsImportVersion7.N_DATEEXPIRED).addText(CmsDateUtil.getHeaderDate(resource.getDateExpired()));
			}
			int resFlags = resource.getFlags();
			resFlags &= ~CmsResource.FLAG_LABELED;
			fileElement.addElement(CmsImportVersion7.N_FLAGS).addText(Integer.toString(resFlags));

			// properties
			Element propertiesElement = fileElement.addElement(CmsImportVersion7.N_PROPERTIES);
			List<CmsProperty> properties = cmsObject.readPropertyObjects(cmsObject.getSitePath(resource), false);
			Collections.sort(properties);
			for (CmsProperty property : properties) {
				if (property == null) {
					continue;
				}
				addPropertyNode(propertiesElement, property.getName(), property.getStructureValue(), false);
				addPropertyNode(propertiesElement, property.getName(), property.getResourceValue(), true);
			}

			// relations
			List<CmsRelation> relations = cmsObject.getRelationsForResource(resource, CmsRelationFilter.TARGETS.filterNotDefinedInContent());
			Element relationsElement = fileElement.addElement(CmsImportVersion7.N_RELATIONS);

			for (CmsRelation relation : relations) {
				CmsResource target;
				try {
					target = relation.getTarget(cmsObject, CmsResourceFilter.ALL);
				}
				// if the relation's target is not found, LOG it and skip
				catch (CmsVfsResourceNotFoundException e) {
					if (LOG.isWarnEnabled()) {
						LOG.warn("relation target " + relation.getTargetPath() + " not found for " + rootPath, e);
					}
					continue;
				}
				addRelationNode(relationsElement, target.getStructureId().toString(), target.getRootPath(), relation.getType().getName());
			}

			// access control
			Element acl = fileElement.addElement(CmsImportVersion7.N_ACCESSCONTROL_ENTRIES);

			// read the access control entries
			List<CmsAccessControlEntry> fileAcEntries = cmsObject.getAccessControlEntries(rootPath, false);

			// create xml elements for each access control entry
			for (CmsAccessControlEntry ace : fileAcEntries) {
				Element accessentry = acl.addElement(CmsImportVersion7.N_ACCESSCONTROL_ENTRY);

				// now check if the principal is a group or a user
				int flags = ace.getFlags();
				String acePrincipalName;
				CmsUUID acePrincipal = ace.getPrincipal();
				if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_ALLOTHERS) > 0) {
					acePrincipalName = CmsAccessControlEntry.PRINCIPAL_ALL_OTHERS_NAME;
				}
				else if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_OVERWRITE_ALL) > 0) {
					acePrincipalName = CmsAccessControlEntry.PRINCIPAL_OVERWRITE_ALL_NAME;
				}
				else if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_GROUP) > 0) {
					// the principal is a group
					acePrincipalName = cmsObject.readGroup(acePrincipal).getPrefixedName();
				}
				else if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_USER) > 0) {
					// the principal is a user
					acePrincipalName = cmsObject.readUser(acePrincipal).getPrefixedName();
				}
				else {
					// the principal is a role
					acePrincipalName = CmsRole.PRINCIPAL_ROLE + "." + CmsRole.valueOfId(acePrincipal).getRoleName();
				}

				accessentry.addElement(CmsImportVersion7.N_ACCESSCONTROL_PRINCIPAL).addText(acePrincipalName);
				accessentry.addElement(CmsImportVersion7.N_FLAGS).addText(Integer.toString(flags));

				Element permissionset = accessentry.addElement(CmsImportVersion7.N_ACCESSCONTROL_PERMISSIONSET);
				permissionset.addElement(CmsImportVersion7.N_ACCESSCONTROL_ALLOWEDPERMISSIONS).addText(Integer.toString(ace.getAllowedPermissions()));
				permissionset.addElement(CmsImportVersion7.N_ACCESSCONTROL_DENIEDPERMISSIONS).addText(Integer.toString(ace.getDeniedPermissions()));
			}

			return fileElement;
		}
		catch (CmsException e) {
			LOG.error("There was a CmsException while trying to genereate the XML info for the resource " + rootPath, e);
			return null;
		}
	}

	/**
	 * Internal method used to convert an XML document to a formatted String.
	 * @param document the Document to be converted.
	 * @return  the formatted String for the given Document.
	 */
	private static String getFormattedStringForDocument(Document document) {
		XMLWriter writer = null;
		ByteArrayOutputStream outputStream = null;
		String xmlString = null;
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setIndentSize(4);
		format.setTrimText(false);
		format.setEncoding(CmsEncoder.ENCODING_UTF_8);

		try {
			outputStream = new ByteArrayOutputStream();
			writer = new XMLWriter(outputStream, format);
			writer.write(document.getRootElement());
			writer.flush();
			xmlString = outputStream.toString(CmsEncoder.ENCODING_UTF_8).trim();
		}
		catch (UnsupportedEncodingException e) {
			// This doesn't happen since UTF-8 is known to be supported
			LOG.error("UTF-8 is not supported", e);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				}
				catch (IOException e) {
					// do nothing
				}
			}
			if (writer != null) {
				try {
					writer.close();
				}
				catch (IOException e) {
					// do nothing
				}
			}
		}
		return xmlString;
	}

}
