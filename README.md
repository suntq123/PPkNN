# PPkNN
This repo is the code for the paper "Privacy-Preserving k-Nearest Neighbor Query: Faster and More Secure". This code is for testing the query response time only.  

## Requirements
* JDK-17

## Get Started
For ease of testing, we have combined the code for the data user and the two servers into one project. The test requires three networked computers C1,  C2 and a client User.
Before running the program, import the project into C1, C2 and User. All parameter configurations can be completed in the main function of /src/main/java/cn/ac/iscas/TestSKNNV2.java. 
Note that the datasets for N=1048576 (/PPkNN/src/main/dataset/gowalla/gowalla-1048576-0.zip, /PPkNN/src/main/dataset/syn/syn-1048576-0.zip) need to be decompressed.
* Parameter setting (For C1, C2 and User)
    * int testType = 2;               // 0-BPPkNN, 2-VPPkNN
    * int datasetType = 1;            // 0-Synthetic dataset, 1-Gowalla dataset
    * int N =1024;                    // dataset size: 1024, 4096, 16384, 65536, 262144, 1048576
    * int dataLength = 12;            // bit length of values: DO NOT MODIFY 
    * int dimension = 2;              // number of dimensions: 2, 5, 10, 15, 20, 25
    * int k = 5;                      // number of nearest neighbors: 1, 5, 10, 15, 20, 25
    * String c1 = "c1 8001";                                // c1 portC1
    * String c2 = "c2 127.0.0.1 8001 8002";                 // c2 ipC1 portC1 portC2
    * String user = "user 127.0.0.1 8001 127.0.0.1 8002 " + ...   // user ipC1 portC1 ipC2 portC2
    * 
## Query Processing
* Computer C1
    * C1 selects the role "c1" by modifying the code in the main function of /src/main/java/cn/ac/iscas/TestSKNNV2.java as follows:  
        ```
        //select a role  
        args = c1.split(" ");  
        //args = c2.split(" ");  
        //args = user.split(" ");
        ```  
    * C1 runs the TestSKNNV2.java.
* Computer C2
    * C2 selects the role "c2" by modifying the code in the main function of /src/main/java/cn/ac/iscas/TestSKNNV2.java as follows:  
        ```
        //select a role  
        //args = c1.split(" ");  
        args = c2.split(" ");  
        //args = user.split(" ");  
        ```
    * C2 runs the TestSKNNV2.java.
* Computer User
    * User selects the role "user" by modifying the code in the main function of /src/main/java/cn/ac/iscas/TestSKNNV2.java as follows: 
        ``` 
        //select a role  
        //args = c1.split(" ");  
        //args = c2.split(" ");  
        args = user.split(" ");  
        ```
    * User runs the TestSKNNV2.java.

