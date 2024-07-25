# PPkNN

## Requirements
* JDK-17

## How to run
For ease of testing, we have combined the code for the user end and two servers into one project. During testing, select a role before running the program. Additionally, to test on different computers, it is necessary to modify the corresponding IP parameters. All parameter configurations can be completed in the main function of the TestSKNNV2.java, and then you can run the TestSKNNV2.java.

* Parameter setting

    * testType: 0-BPPkNN  2-VPPkNN
    * datasetType: 0-Synthetic dataset  1-Real world dataset
    * N: dataset size
    * dataLength: the bit length of numerical values
    * dimension: dimension of data
    * k: k in kNN