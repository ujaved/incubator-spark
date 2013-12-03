ins = open( "sm_data.txt", "r" )#mr_wiki_data16
data1=[]
data2=[]
counter=0
for line in ins:
	#line=line.replace("\n","")
	if counter==0:
		data1.append(line)
		#print line
		counter=1
	else: 
		if counter==1:
			data2.append(line)
			counter=0
	
    
ins.close()

out1 = open("sm_data1.txt","w")
out1.writelines(data1)
out1.close()

out1 = open("sm_data2.txt","w")
out1.writelines(data2)
out1.close()