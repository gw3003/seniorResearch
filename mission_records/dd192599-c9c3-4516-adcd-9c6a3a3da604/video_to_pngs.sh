#! To extract individual frames from the mp4
mkdir video_frames
ffmpeg -i "video.mp4" video_frames/frame_%06d.png
