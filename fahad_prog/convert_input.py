ins = open( "links-simple-sorted.txt", "r" )
for line in ins:
    line=line.replace("\n","")
    line_arg=line.split(":")
    args=line_arg[1].split( )
    for a in args:
    	print line_arg[0].strip()," ",a.strip()
ins.close()