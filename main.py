import cv2
from ultralytics import YOLO
from fastapi import UploadFile, FastAPI, Response, Request
from fastapi.responses import FileResponse
from starlette.status import HTTP_400_BAD_REQUEST
import os 
import time
import re
import hashlib


COLOR_RED = (0, 0, 255)
COLOR_GREEN = (0, 255, 0)

model = YOLO("model1.pt")
model.fuse()
app = FastAPI()


def clamp(val, a, b):
    if val  > b:
        return b
    elif val < a:
        return a
    return val

@app.post("/upload/")
async def detect_objects(req: Request):
    contents = await req.body()

    id = hashlib.md5(string=contents, usedforsecurity=False).hexdigest() #poxyi

    test = open(f'{id}.mp4', 'wb')
    test.write(contents)
    test.close()

    print(f'Successfully uploaded a file with id = {id}', f'\tFile size = {len(contents)}', sep='\n')

    return {"id": id}


@app.delete('/detection/')
def delete_file(id: str, response: Response):
    try:
        os.remove(f'{id}_det.webm')
        return {'message': 'ok'}
    except Exception as e:
        response.status_code = HTTP_400_BAD_REQUEST
        return {'message:': ''.join(map(str, e.args))}


@app.get('/detection/')
def get_detection(id: str):
    det_path = f'{id}_det.webm'
    if os.path.exists(det_path):
        return FileResponse(path=det_path, media_type='video/webm')

    cap = cv2.VideoCapture(f'{id}.mp4')
    w, h, fps = (int(cap.get(x)) for x in (cv2.CAP_PROP_FRAME_WIDTH, cv2.CAP_PROP_FRAME_HEIGHT, cv2.CAP_PROP_FPS))
    det = cv2.VideoWriter(det_path, cv2.VideoWriter_fourcc(*"VP90"), fps, (w, h))
    line_w = int(min(w, h) / 75)
    aggression_end_time = time.time()

    while cap.isOpened():
        success, frame = cap.read()
        if success:
            results = model.predict(frame, batch=8, imgsz=(640, 640))
            classes = results[0].boxes.cls.cpu().numpy().astype(int)
            for id in classes:
                if model.model.names[id] == 'aggression':
                    aggression_end_time = time.time() + 0.5
                    break

            is_red  = aggression_end_time - time.time() > 0
            cv2.rectangle(frame, (0, 0), (w, h,), COLOR_RED if is_red else COLOR_GREEN, line_w)
            det.write(frame)
        else:
            break

    cap.release()
    det.release()
    cv2.destroyAllWindows()

    return FileResponse(path=det_path, media_type='video/webm')