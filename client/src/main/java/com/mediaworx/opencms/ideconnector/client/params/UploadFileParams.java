package com.mediaworx.opencms.ideconnector.client.params;

import org.apache.commons.fileupload.FileItem;


/**
 * TODOC
 * <p/>
 * (c) 2015, mediaworx berlin AG
 * All rights reserved
 * <p/>
 *
 * @author initial author: Kai Widmann <widmann@mediaworx.com>, 03.03.2015
 * @author last editor: $Author: kwidmann $
 * @version $Revision: 2449 $
 */
public class UploadFileParams extends GenericParams {

	FileItem uploadFileItem;

	public FileItem getUploadFileItem() {
		return uploadFileItem;
	}

	public void setUploadFileItem(FileItem uploadFileItem) {
		this.uploadFileItem = uploadFileItem;
	}
}
