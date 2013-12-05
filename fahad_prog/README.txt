Download data from http://users.on.net/~henry/pagerank/links-simple-sorted.zip given on page: http://haselgrove.id.au/wikipedia.htm

Run convert_input.py to convert the data from downloaded file to a format which will be accepted by page rank example in scala
You can download the converted data from http://punjab.cs.washington.edu/cse522/ too

Run data_split.py to even split the data into two files

Edit src/main/scala/SimpleApp.scala to take these files (output of data_split.py) as input. Look for iter variable which defines the number of iterations it is going to run

Compile the applicatin
	sbt package
	sbt run

sm_data.txt is a very small sample data which scala page rank example would accept

All the filenames are hard coded in the code. 
