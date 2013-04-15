/**
 * PhantomJS script responsible for pre-interpreting javascript in a
 * given page. This script creates a webserver running behind a  port
 * specified on the command line (defaulting to 8888). 
 * 
 * The server can respond to commands sent to it by POST requests containing
 * a "command" field in their body. The command can be "ping", to which the 
 * server replies "pong", or "exit", which causes the server to exit.
 * 
 * TODO: check source of received command b/f exiting 
 */
var system = require('system')
var server = require('webserver').create()

var port = 8888

if (system.args.length === 1) {
    console.log('No port provided, defaulting to ' + port);
}
else {
    port = system.args[1]
}



var service = server.listen(port, function(request, response) {
	
	/**
	 * Should correspond to a command request
	 */
	if(request.method == "POST") {
		if(request.post.command == 'ping') {			
			response.statusCode = 200
			response.write('pong')
			response.close()
		} else if(request.post.command == 'exit') {
			console.log('exit command received')
			
			response.statusCode = 200
			response.write('exiting')
			response.close()
			
			setTimeout('shutdownServer()', 0) // Prevents client from receiving 403 Directory Listing Denied
			
		} else {
			console.log('invalid command received')
			
			response.statusCode = 400 // Bad Request
			response.close()
		}
	}
	
	/**
	 * Normal request, containing a URI corresponding to a page to interpret.
	 */
	if(request.method == "GET") {
		var flag = false
		var page = require('webpage').create()
		var url = decodeURIComponent(request.url).substring(1)

		// Fix the URLs by adding the missing "/" swallowed by the URI decoding
		if(url.substring(0,6) == 'http:/') {	
			url = 'http://' + url.substring(6)
			flag = true
		}
		if(url.substring(0,7) == 'https:/') {	
			url = 'https://' + url.substring(7)
			flag = true
		}
		
		// If the requested URI is absolute, retrieve and interpret the corresponding page
		if(flag) {
			console.log('** ' + url + ' **')

			page.open(url, function() {
				response.statusCode = 200
				response.write(page.content)
		    		response.close();
			});
		// Otherwise, returns a 204: No Content response, to prevent clients from waiting
		}  else {
			console.log('-- ' + url + ' --')
			
			response.statusCode = 204 // No Content
			response.close()
		}
	}
	
	
	
});

function shutdownServer() { 
	server.close()
	phantom.exit()
}
