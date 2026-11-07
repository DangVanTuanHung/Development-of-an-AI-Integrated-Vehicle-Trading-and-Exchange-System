"""Synthetic image bytes for isolated API tests only; never seed public demo cards."""
import struct,zlib
def photo_fixture():
 width,height=640,480
 def chunk(kind,data):return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
 # Two distinguishable regions exercise dimensions/blank-image checks.
 rows=b''.join(b'\x00'+bytes((30,80,120) if y<height//2 else (190,140,90))*width for y in range(height))
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',width,height,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(rows))+chunk(b'IEND',b'')
