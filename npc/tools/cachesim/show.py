import re

import matplotlib.pyplot as plt
import numpy as np
from mpl_toolkits.mplot3d import Axes3D

pattern = re.compile(
    r"\s*set=(\d+),way=(\d+),size=(\d+)\s*hit=(\d+) miss=(\d+) cmiss=(\d+)"
)

lway = []
lset = []
lsize = []
lmiss = []
lhit = []

# read the results
results = ["cache_result"]
for fileName in results:
    file = open(fileName)
    for line in file:
        match = pattern.search(line)
        if match:
            print(match.groups())
            rset, rway, rsize, rhit, rmiss, rcmiss = match.groups()
            lway.append(int(rway))
            lset.append(int(rset))
            lsize.append(int(rsize))
            lmiss.append(float(rmiss) / (float(rmiss) + float(rhit)))
            lhit.append(float(rhit) / (float(rmiss) + float(rhit)))

# data
ways = np.array(lway)
sets = np.array(lset)
sizes = np.array(lsize)

miss_probabilities = np.array(lmiss)

point_sizes= np.array(lhit)*50 +5

# 创建3D图形
fig = plt.figure()
ax = fig.add_subplot(111, projection="3d")

# 绘制散点图
sc = ax.scatter(
    ways,
    sets,
    sizes,
    c=miss_probabilities,
    # cmap="viridis",
    s=point_sizes
)


# 添加颜色条
cbar = plt.colorbar(sc)
cbar.set_label("Miss Probability")

# 设置轴标签
ax.set_xlabel("Ways")
ax.set_ylabel("Sets")
ax.set_zlabel("Cache Size")

# 显示图形
plt.show()
