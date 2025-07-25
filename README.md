# TCP-IP-Java-Model
*Version 1.0.0 — Last updated: July 13, 2025*
## Sections
- [Introduction](#introduction)
- [Background](#background)
- [Features](#features)
- [Setup](#setup)
- [Configuration](#configuration)
- [Running the Project](#running-the-project)
- [Example Usage](#example-usage)
- [Future Plans](#future-plans)
- [Contact](#contact)

---

## Introduction
This is a Java model simulating TCP communication and other protocols used in communication. This is a very simple model, using one router and a user-defined amount of clients which may connect to it, all of which will run on your machine. This is supposed to showcase the fundamentals of TCP (Ordering and error checking), how clients connect to a network, and how routers switch packets.

## Background
This is a passion project that I started in my second year of University. I was inspired by a module I had taken called Computer Networks which introduced me to the OSI model and TCP/IP. I was fascinated with the way the internet worked and I wanted to apply the knowledge that I had gained on this course into a practical scenario.
This project has taught me a lot about the nuances of sending data across networks. Initially I wanted to create a very modular system where one could simulate a wider network with several routers and underlying clients, however I quickly realised how difficult this would be given the resources I have and the difficulty of the task, thus I scaled back to just one configurable network which has still taught me a lot.

This project was good practice for me to get familiar with concurrency, threading, and socket communication on top of TCP communication and other protocols

## Features

- Basic TCP simulation with error detection and retransmission.
- Simulated packet loss and corruption.
- Basic DHCP simulation.
- A Java-Swing web chat interface.
- Configurable elements  to the network.

## Setup
This is how you set up the project on your machine. These instructions are assuming you are retrieving this project from my github.
IF YOU'RE READING THIS FROM GITHUB, FOLLOW THESE STEPS. OTHERWISE SKIP TO 'RUNNING THE PROJECT'.

1. Click the green '<> Code' Button on the top right.
2. Press 'Download as ZIP'
3. Unzip the file
        
Et voila! If you wish to configure the project to satisfy your needs, read the section below.


## Configuration
You can find Configuration in \TCP-IP-JAVA-MODEL\SimUtils\SimConfig.java Here you will find different variables which you can change that can change the behaviour project-wide. These variables are as follows:

1. addressAmount - This is an INTEGER value. This determines how many times the loop in Router.createAddresses will run. The value cannot be higher than 254 (Not 255 because the router takes up address space x.x.x.1). It is recommended to keep this value at a reasonable number i.e. a number that suits your testing needs. The default value is 4.
2. routerHost - This is a STRING value which determines the host in which the Router object actually runs. By default it is "0.0.00". If this is causing issues, try changing the host to 'localhost'.
3. routerPort - This is an INTEGER value which determines the port which the router object uses for Socket communication. By default it is 12345
4. networkIP - This is a STRING value which determines the SIMULATED network IP prefix. This will affect the IPs assigned to the clients during the DHCP process and the IPs shown throughout the simulation.
5. errorChance - This is a DOUBLE value which determines the chances of TCP packets being corrupted. This value must be 0<= and >1. For NO error simulation, change this value to 0. The default value is 0.1 (10% chance of corruption.)
6. dropChance - This is a DOUBLE value which determines the chances of TCP packets being dropped in order to simulate packet loss. This value must be 0<= and >1. For NO packet loss simulation change this value to 0. The default is 0.2 (20% chance of loss)
7. threadAmount - This is an INTEGER value which determines the size of the threadpool which handles retransmissions. These threadpools are created PER CLIENT. So any tests where a larger amount of clients is ran will ideally need to see this number reduced as it will have an affect on performance. A result of this, however, may be that retransmissions are handled slower as only the threadAmount of transmissions can be handled concurrently while the rest of them wait in queue. This value must be >=1. The default is 4.


## Running The Project
This is how you run the project. For each router and client instance, you will need a new terminal window. Windows command line or powershell will be fine.
1. IN TERMINAL, ENTER THE PROJECT DIRECTORY e.g. 'cd C:\Users\User\TCP-IP-Java-Model'
2. COMPILE ALL THE FILES VIA 'javac *.java'
3. START THE ROUTER VIA 'java Router' (case sensitive)
4. OPEN A NEW TERMINAL WINDOW, MAKE SURE YOU ARE IN THE PROJECT DIRECTORY, START A CLIENT VIA 'java Client [hostname]' where [hostname] is the you wish to give this particular client. E.G. 'java Client Alice' or 'java Client Bob'.
   5. FOR EACH NEW CLIENT, REPEAT STEP 4.


## Example Usage
1. Start Router.
2. Start Client Alice.
3. Start Client Bob. At this point you will have two swing windows open. For ease I will prefix anything that happens in Alice's window with [ALICE] and anything that happens in Bob's window with [BOB].
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
---

## Future Plans
At the minute this project will be put on a back burner.

In the future I would like to make this project easier to use for less technically-skilled users.  This will include creating an executable file with a ‘Master GUI’ which will essentially allow the user to control their network and see how it works without having to go into the terminal. I feel like if done right, this can become a very useful educational tool to introduce the concept of TCP communication.

## Contact
GitHub - @radekzien

LinkedIn - https://www.linkedin.com/in/radoslaw-zienkiewicz/
