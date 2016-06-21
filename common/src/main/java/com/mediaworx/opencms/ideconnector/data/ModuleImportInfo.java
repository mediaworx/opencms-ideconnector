package com.mediaworx.opencms.ideconnector.data;

/**
 * Each module needs two things to be imported:
 * <ul>
 *     <li>The path to the module's zip file (moduleZipPath)</li>
 *     <li>The site root to which the module should be imported to (importSiteRoot)</li>
 * </ul>
 */
public class ModuleImportInfo {

	/** The path to the module's zip file */
	private String moduleZipPath;

	/** The site root to which the module should be imported to */
	private String importSiteRoot;

	/**
	 * @return The path to the module's zip file
	 */
	public String getModuleZipPath() {
		return moduleZipPath;
	}

	/**
	 * @param moduleZipPath The path to the module's zip file
	 */
	public void setModuleZipPath(String moduleZipPath) {
		this.moduleZipPath = moduleZipPath;
	}

	/**
	 * @return The site root to which the module should be imported to
	 */
	public String getImportSiteRoot() {
		return importSiteRoot != null && importSiteRoot.length() > 0 ? importSiteRoot : "/";
	}

	/**
	 * @param importSiteRoot The site root to which the module should be imported to
	 */
	public void setImportSiteRoot(String importSiteRoot) {
		this.importSiteRoot = importSiteRoot;
	}
}
