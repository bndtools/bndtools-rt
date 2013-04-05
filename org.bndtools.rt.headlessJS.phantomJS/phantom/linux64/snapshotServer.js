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
	if(request.method == "POST") {
		if(request.post.command == 'ping') {
			console.log('ping received')
			
			response.statusCode = 200
			response.write('pong')
			response.close()
		} else if(request.post.command == 'exit') {
			console.log('exit command received')
			
			response.statusCode = 200
			response.write('exitting')
			response.close()
			
			setTimeout('shutdownServer()', 0) // Prevents client from receiving 403 Directory Listing Denied
			
		} else {
			console.log('invalid command received')
			
			response.statusCode = 400 // Bad Request
			response.close()
		}
	}
	
	if(request.method == "GET") {
		var flag = false
		var page = require('webpage').create()
		var url = decodeURIComponent(request.url).substring(1)
		console.log('-- ' + url + ' --')

		if(url.substring(0,6) == 'http:/') {	
			url = 'http://' + url.substring(6)
			flag = true
		}
		if(url.substring(0,7) == 'https:/') {	
			url = 'https://' + url.substring(7)
			flag = true
		}
		if(flag) {
			console.log('** ' + url + ' **')

			page.open(url, function() {
				response.statusCode = 200
				response.write(page.content)
		    		response.close();
			});
		}  else {
			response.statusCode = 204 // No Content
			response.close()
		}
	}
	
	
	
});

function shutdownServer() { 
	server.close()
	phantom.exit()
}
