# UdpChat

rz2357
Ruomeng Zhang

## Makefile
Make a new project:
```bash
make new
```

Compile the project:
```bash
make build
```

Create executable jar file:
```bash
make jar
```

## Run program
To open a server on port:
```bash
java -jar UdpChat.jar -s <port>
```
To login as a client using client name, server IP address, server port and loacal listening port:
```bash
java -jar UdpChat.jar -c <client-name> <server-ip> <server-port> <local-port>
```

## Features
### Registration
The registration function can be used when user login and register. No space ` ` in username.

As a client, use command `-c` to tell the program `UdpChat` the identification of user. Then add some more info about the client, including user name, user's port, server IP address, and server port.

If the user has logged in to the system, the old logged in client will be forced logging out by server.

Server is holding an table of all registered users, including their listening port, address, and state.

### Chatting
User can chat with other users by their user's name. Using the format `send <user-name> <message>` to send a message to that user. 

User cannot send a message to itself.

User will get a acknowledge ACK from the receiver. 

User cannot send a message to a unknown user.

If the receiver is offline, user won't recieve ACK message, thus, user will send the offline message to server.

If server is offline too, user will exit.

### De-registering
Clients can use command `reg <user-name>` and `dereg <user-name>` to register and de-register from a client. The user can only de-rigister itself, in case there will be some conflictions between the user and other users.

Once a user de-rigistered, it will send a message to server in order to show it's state. The other users will get table updating message from the server and update the server.

### Offline Chat
Once the client re-login, it should get the offline messages from others.

## Algorithms
Both client and server implement multi-thread to listen to their port all the time and send message from another port.

To save the table, I create a User class, which can add new user, save offline messages, update state, update address and port ect.

The program uses UDP to send messages, and I decorate the messages with different headers to imply different usages of the command.

E.g:

For server:
`reg#!<user>` means a user registered.<br>
`dereg#!<user>` means a user de-registered.<br>
`save#!<fromUser>&!<toUser>&!<message content>` means get a offline message need to be saved.<br>
`new#!<user>&!<IPaddress>&!<port>` means a new user registered.

For client:
`msg#!<message>` means a message come.<br>
`off#!<offline>` means offline messages.<br>
`update#!<table>` means server updates the user table.<br>
`conf#!` means there is a confliction of the user, user must log out.


