import sys, json, zipfile, collections
sys.path.insert(0,'/tmp/work/stubs')
from classfile import ClassFile
ar=collections.Counter()
def parse_type(s,i):
    c=s[i]
    if c in 'BCDFIJSZV': return i+1
    if c=='[': return parse_type(s,i+1)
    if c=='T': return s.index(';',i)+1
    if c in '+-': return parse_type(s,i+1)
    if c=='*': return i+1
    if c=='L':
        j=i+1; name=''
        while True:
            ch=s[j]
            if ch=='<':
                n=0; j+=1
                while s[j]!='>':
                    j=parse_type(s,j); n+=1
                ar[name]=max(ar[name],n); j+=1
            elif ch=='.':
                name+='$'; j+=1
            elif ch==';':
                return j+1
            else:
                name+=ch; j+=1
    raise ValueError(s[i:])
def parse_sig(s):
    i=0
    if s[0]=='<':  # formal type params
        i=1
        while s[i]!='>':
            j=s.index(':',i); i=j
            while s[i]==':':
                i+=1
                if s[i] in 'LT[': i=parse_type(s,i)
        i+=1
    while i<len(s):
        if s[i] in '()^': i+=1; continue
        i=parse_type(s,i)
z=zipfile.ZipFile('/tmp/work/spraute-mojmap.jar')
for n in z.namelist():
    if not n.endswith('.class'): continue
    cf=ClassFile(z.read(n))
    sigs=[cf.signature()]+[cf.signature(m.attrs) for m in cf.fields+cf.methods]
    # LocalVariableTypeTable too
    for m in cf.methods:
        code=m.attrs.get('Code')
    for s in sigs:
        if s:
            try: parse_sig(s)
            except Exception as e: print('ERR',n,s,e)
json.dump(ar,open('/tmp/work/stubs/sig_arity.json','w'),indent=0)
print(len(ar), [(k,v) for k,v in ar.items() if k.startswith(('net/minecraftforge','com/mojang/data'))][:40])
