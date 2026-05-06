# csc460-project-4

# COMPILATION AND EXECUTION INSTRUCTIONS
(ensure you have the Oracle JDBC driver added to your classpath before compiling: `export CLASSPATH=/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar:${CLASSPATH}`)
1. Compile --   `javac LLMEcosystem.java`
2. Run --       `java LLMEcosystem`
3. Program --   Follow the on screen UI to test any funcitonalities (1..8) or queries (9..12) provide by the program

NOTE: Don't exit using ctrl c, as it has been causing problems with transactions and may be unable
	  to add users after you do this. Please use the 0 key selection to exit, and properly close the connection.
	  Because of the many id prompts, reccomended to have two terminals side by side, one with sqlpl loaded to see the 
	  tables and use correct IDs and the other to run the application program.


# WORKLOAD DISTRIBUTION BETWEEN TEAM MEMBERS
Luis Miranda:
    - Required functionalities 7 & 8        (LLMEcosystem.java lines 1218-1461)
    - E-R Diagram Draft                     (Due date #2 deliverable)
Shale Van Cleve:
    - Required functionalities 5 & 6        (LLMEcosystem.java lines 916-1216)
    - Required queries                      (LLMEcosystem.java lines 1463-1635)
    - Program UI                            (LLMEcosystem.java lines 1637-EOF)
    - createTables.sql                      (script that initializes the actual DB)
    - E-R Diagram Draft                     (Due date #2 deliverable)
    - design.pdf parts (ii) & (iv)          (Part b of due date #3 deliverables)
    - README.txt                            (Part c of due date #3 deliverables)
Zoltan Kotha:
    - Required functionalities 1 & 2        (LLMEcosystem.java lines 63-351)
    - createTables.sql                      (script that initializes the actual DB)
    - E-R Diagram Draft                     (Due date #2 deliverable)
    - design.pdf parts (i) & (iii)          (Part b of due date #3 deliverables)
Ian Sanchez Lopez:
    - Required functionalities 3 & 4        (LLMEcosystem.java lines 353-914)
    - E-R Diagram Draft                     (Due date #2 deliverable)
	
