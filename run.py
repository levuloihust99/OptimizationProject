import os
import subprocess

input_files = []
for folder in os.listdir("data"):
    for filename in os.listdir(os.path.join("data", folder)):
        path = os.path.join("data", folder, filename)
        input_files.append(path)

for path in input_files:
    for i in range(10):
        # command = f" -m main --input {path}"
        command = ['python', '-m', 'main', '--input', path, '--id', str(i)]
        # print(f"Execute command: {command}")
        process = subprocess.Popen(command)
        process.wait()
