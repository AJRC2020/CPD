# Project Instructions

Before anything, our current commit has a problem with the usage of packages, creating an unecessary second package on the server folder which caused some compilation errors. And because I didn't know if I could change it after delivery, I left as is.

The directories for each node are created automatically. There is no need to create them.

But after fixing the packages issues (Note that the commands will change depending on how the packages were fixed, due to changing the origin folder)

Compiling: javac g07/assign2/src/*.java

Running:
1. java Store <node_id> <Store_port> <IP_mcast_addr> <IP_mcast_port>

examples: 

        java g07.assign2.src.Store 127.0.0.1 8080 228.5.6.7 6789
        java g07.assign2.src.Store 127.0.0.2 8080 228.5.6.7 6789 
        java g07.assign2.src.Store 127.0.0.3 8080 228.5.6.7 6789

2. java TestClient <node_ap> \<operation> \<opnd>

examples:   

        java g07.assign2.src.TestClient 127.0.0.1 8080 join 
        java g07.assign2.src.TestClient 127.0.0.1 8080 leave 
        java g07.assign2.src.TestClient 127.0.0.1 8080 put file.txt 
        java g07.assign2.src.TestClient 127.0.0.1 8080 get key 
        java g07.assign2.src.TestClient 127.0.0.1 8080 delete key
