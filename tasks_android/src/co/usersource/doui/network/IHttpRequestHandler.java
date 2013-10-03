/**
 * 
 */
package co.usersource.doui.network;

import org.apache.http.HttpResponse;


/**
 * @author rsh
 *
 */
public interface IHttpRequestHandler {

	void onRequest(HttpResponse response);

}
