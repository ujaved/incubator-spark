import math
file1 = open( "res6_1.txt", "r" ) #Split 1 result
file2 = open( "res6_2.txt", "r" ) #Split 2 result
file3 = open( "res6_0.txt", "w" ) #write merged results
file4 = open( "res6.txt", "r" ) #baseline result
result={}
r_max=0
r_min=10

for line in file1:
    line=line.replace("\n","")
    line_arg=line.split(",")
    result[line_arg[0]]=float(line_arg[1])
for line in file2:
	line=line.replace("\n","")
	line_arg=line.split(",")
	if(result.get(line_arg[0],None)==None):
		result[line_arg[0]]=float(line_arg[1])
	else:
		result[line_arg[0]]=(result[line_arg[0]]+float(line_arg[1]))/2
for key in result.keys():
	file3.write(key+","+str(result[key])+"\n")
	if result[key]<r_min:r_min=result[key]
	if result[key]>r_max:r_max=result[key]
file3.close()
mse_sum=0
mse_n=0
for line in file4:
	line=line.replace("\n","")
	line_arg=line.split(",")
	mse_sum=mse_sum+((result.get(line_arg[0],1.0)-float(line_arg[1])) ** 2)
	mse_n=mse_n+1
mse_t=mse_sum/mse_n
rmse=math.sqrt(mse_t)
print("RMSE:"+str(rmse))
nrmse=rmse/(r_max-r_min)
print("NRMSE:"+str(nrmse))
#print result
file1.close()
file2.close()
file4.close()