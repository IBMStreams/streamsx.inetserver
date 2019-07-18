## Example using HTTPRequestProcess, tuple version.

The application posts a form to the browser, prompting for input. Form submission 
passes parameters to the HTTPRequestProcess operator and on into the Streams application. 

Streams builds a response and sends it back out the originating HTTPRequestResponse
operator. 

When the Streams application is running application is accessed via 
http://localhost:8080/livepage
