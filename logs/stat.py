import numpy as np

obj = np.array([1597,1622,1581,1584,1629,1591,1600,1610,1603,1618])
print("std_dev = {}".format(np.std(obj)))
print("f_avg = {}".format(np.mean(obj)))
print("f_min = {}".format(np.min(obj)))
print("f_max = {}".format(np.max(obj)))
t = np.array([85,136,68,86,73,68,47,44,38,66])
print("t_avg = {}".format(np.mean(t)))