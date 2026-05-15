import socket

def test():
    boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    photoId = "testid"
    
    body = (
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"photo_id\"\r\n\r\n"
        f"{photoId}\r\n"
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"file\"; filename=\"{photoId}.jpg\"\r\n"
        f"Content-Type: image/jpeg\r\n\r\n"
        f"dummydata"
        f"\r\n--{boundary}--\r\n"
    ).encode('utf-8')
    
    req = (
        f"POST /sync/photos HTTP/1.1\r\n"
        f"Host: localhost:8000\r\n"
        f"Content-Type: multipart/form-data; boundary={boundary}\r\n"
        f"Content-Length: {len(body)}\r\n"
        f"\r\n"
    ).encode('utf-8') + body

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('localhost', 8000))
    s.sendall(req)
    print(s.recv(4096).decode('utf-8'))
    s.close()

test()
