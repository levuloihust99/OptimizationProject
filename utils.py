import random
import numpy as np


def read_input(filepath: str):
    text = open(filepath).read()
    text = text.split('\n')
    for i in range(len(text)):
        text[i] = text[i].split(" ")
        for j in range(len(text[i])):
            text[i][j] = int(text[i][j])
    text[1] = [-1] + text[1]
    return text[0], np.array(text[1]), np.array(text[2:])


def random_chunk(s, n: int):
    random.shuffle(s)
    batch_size = len(s) // n
    results = []
    for i in range(n - 1):
        results.append(s[batch_size * i:batch_size * (i + 1)])
    results.append(s[batch_size * (n - 1):])
    return results


def clone(s):
    t = []
    for ll in s:
        t.append(ll.copy())
    return t
