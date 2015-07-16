package com.mediaworx.opencms.ideconnector.client.exceptions;

/**
 * TODOC
 * <p/>
 * (c) 2015, mediaworx berlin AG
 * All rights reserved
 * <p/>
 *
 * @author initial author: Kai Widmann <widmann@mediaworx.com>, 22.01.2015
 * @author last editor: $Author: kwidmann $
 * @version $Revision: 2670 $
 */
public class NotFoundException extends ConnectorException {

	public NotFoundException(String message, String responseBody) {
		super(message, 404, responseBody);
	}

}
