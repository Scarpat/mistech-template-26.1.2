import sys,os
from PIL import Image,ImageDraw,ImageFont
def sheet(paths,out,cell=96,cols=10,bg=(60,60,60)):
    font=ImageFont.load_default()
    rows=(len(paths)+cols-1)//cols
    W=cols*(cell+8); H=rows*(cell+22)
    S=Image.new('RGB',(W,H),bg); d=ImageDraw.Draw(S)
    for i,p in enumerate(paths):
        try: im=Image.open(p).convert('RGBA')
        except Exception as e: continue
        w,h=im.size
        if h>w*1.5: im=im.crop((0,0,w,w)); w,h=im.size  # animated strip: first frame
        k=max(1,min(cell//w,cell//h)) if max(w,h)<=cell else None
        im = im.resize((w*k,h*k),Image.NEAREST) if k else im.resize((cell,int(cell*h/w)) if w>=h else (int(cell*w/h),cell),Image.NEAREST)
        x=(i%cols)*(cell+8)+4; y=(i//cols)*(cell+22)+2
        # checker
        ch=Image.new('RGB',(cell,cell),(90,90,90)); cd=ImageDraw.Draw(ch)
        for a in range(0,cell,8):
            for b in range(0,cell,8):
                if (a//8+b//8)%2: cd.rectangle([a,b,a+7,b+7],fill=(110,110,110))
        S.paste(ch,(x,y)); S.paste(im,(x+(cell-im.size[0])//2,y+(cell-im.size[1])//2),im)
        n=os.path.splitext(os.path.basename(p))[0][:17]
        d.text((x,y+cell+3),n,fill=(230,230,230),font=font)
    S.save(out)
if __name__=='__main__':
    out=sys.argv[1]; cell=int(sys.argv[2]); cols=int(sys.argv[3]); sheet(sys.argv[4:],out,cell,cols)
