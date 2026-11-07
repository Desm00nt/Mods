import os, re, json, collections, html
root='/tmp/maps/zmap/1.20.1'
cell=re.compile(r'<td class="D ([EGHIJ])"></td><td class="F">(.*?)</td>', re.S)
tag=re.compile(r'<[^>]+>')
inner=re.compile(r'<table><tbody>(.*?)</tbody></table>', re.S)
fmap=collections.defaultdict(set); mmap=collections.defaultdict(set)
classes={}
for dp,_,fs in os.walk(root):
    for f in fs:
        if not f.endswith('.html') or f in('index.html','licenses.html'): continue
        s=open(os.path.join(dp,f),encoding='utf-8').read()
        # class header table is the first <table><tbody> with cells
        for k,tb in enumerate(inner.finditer(s)):
            cells={}
            for m in cell.finditer(tb.group(1)):
                cells[m.group(1)]=html.unescape(tag.sub('',m.group(2))).strip()
            if 'J' not in cells or 'G' not in cells: continue
            j=cells['J']; g=cells['G']
            if k==0 and '(' not in j and j.startswith('net.minecraft.src.C_'):
                classes[j]=g; continue
            jn=j.split('(')[0]; gn=g.split('(')[0]
            if re.fullmatch(r'f_\d+_',jn): fmap[jn].add(gn)
            elif re.fullmatch(r'm_\d+_',jn): mmap[jn].add(gn)
conf={k:v for k,v in list(fmap.items())+list(mmap.items()) if len(v)>1}
print('fields',len(fmap),'methods',len(mmap),'classes',len(classes),'conflicts',len(conf))
for k,v in list(conf.items())[:20]: print(' conflict',k,v)
json.dump({'fields':{k:sorted(v)[0] for k,v in fmap.items()},'methods':{k:sorted(v)[0] for k,v in mmap.items()}},open('/tmp/work/srg2moj.json','w'))
