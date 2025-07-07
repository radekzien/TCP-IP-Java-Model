# TCP-IP-Java-Model
This is a Java model simulating TCP communication and other protocols used in communication. This is a very simple model, using one router and a user-defined amount of clients which may connect to it, all of which will run on your machine. This is supposed to showcase the fundamentals of TCP (Ordering and error checking), how clients connect to a network, and how routers switch packets.

## ----- SETTING UP THE PROJECT -----
This is how you set up the project on your machine. These instructions are assuming you are retrieving this project from my github. If not, please contact the author ASAP. Contact details can be found on @radekzien on github.

**IF YOU'RE READING THIS FROM GITHUB, FOLLOW THESE STEPS. OTHERWISE SKIP TO 'RUNNING THE PROJECT'**
1. Click the green '<> Code' Button on the top right.
2. Press 'Download as ZIP'
3. Unzip the file

Et voila! If you wish to configure the project to satisfy your needs, read the section below.

## ----- CONFIGURING THE PROJECT -----
You can find Configuration in \TCP-IP-JAVA-MODEL\SimUtils\SimConfig.java
Here you will find different variables which you can change that can change the behaviour project-wide.
These variables are as follows:

1. addressAmount - This is an INTEGER value. This determines how many times the loop in Router.createAddresses
    will run. The value cannot be higher than 254 (Not 255 because the router takes up address space x.x.x.1). It is
    recommended to keep this value at a reasonable number i.e. a number that suits your testing needs. The default value is 4.

2. routerHost - This is a STRING value which determines the host in which the Router object actually runs. By default it is "0.0.00". If this is causing issues, try changing     the host to 'localhost'.

3. routerPort - This is an INTEGER value which determines the port which the router object uses for Socket communication. By default it is 12345

4. networkIP - This is a STRING value which determines the SIMULATED network IP prefix. This will affect the IPs assigned to the clients during the DHCP process and the IPs     shown throughout the simulation.

5. errorChance - This is a DOUBLE value which determines the chances of TCP or TCP-ACK packets being corrupted. This value must be  0<= and >1. For NO error simulation,         change this value to 0. The default value is 0.1 (10% chance of corruption.)

## ----- RUNNING THE PROJECT -----
This is how you run the project. For each router and client instance, you will need a new terminal window. Windows command line or powershell will be fine.
1. IN TERMINAL, ENTER THE PROJECT DIRECTORY e.g. 'cd C:\Users\User\TCP-IP-Java-Model'
2. COMPILE ALL THE FILES VIA 'javac *.java'
3. START THE ROUTER VIA 'java Router' (case sensitive)
4. OPEN A NEW TERMINAL WINDOW, MAKE SURE YOU ARE IN THE PROJECT DIRECTORY, START A CLIENT VIA 'java Client [hostname]' where [hostname] is the you wish to give this particular client. E.G. 'java Client Alice' or 'java Client Bob'.
5. FOR EACH NEW CLIENT, REPEAT STEP 4.

## ----- USING THE PROJECT -----
This is an example usage of the project.
1. Start Router.
2. Start Client Alice.
3. Start Client Bob.
At this point you will have two swing windows open. For ease I will prefix anything that happens in Alice's window with [ALICE] and anything that happens in Bob's window with [BOB].
4. [ALICE] Select Bob's name and IP and press 'Open Chat'.
5. [BOB] Select Alice's name and IP and press 'Open Chat'.
6. Exchange messages between each other by typing into the chatbox and pressing 'Send'.
7. Observe each terminal to see how each client behaves, what types of Packets are sent or received, and what's happening with them.
8. At this point you may want to introduce a new client for e.g. Alice to talk to.
9. [ALICE] Press 'BACK' on chat with bob.
10. [ALICE] Select new client's name and IP and press 'Open Chat'.
11. Exchange messages.
12. [ALICE] Press 'x' on the top right of the window.
13. You may want to look at Alice's and the Router's terminal at this point to see the disconnection process.
14. Do the same for all other Clients.
15. Ctrl + C in the Router's terminal to end the process.
