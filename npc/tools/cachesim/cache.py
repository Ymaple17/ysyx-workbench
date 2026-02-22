import math
from abc import ABC, abstractmethod
from collections import OrderedDict
# from functools import lru_cache
# import threading
from multiprocessing import Pool

################################################
#
#    way1    ||  way2
#  ----------------------
#  |tag| data||tag| data| data-size
#  |tag| data||tag| data|
#  |tag| data||tag| data|  sets
#  |tag| data||tag| data|
#  ----------------------
#
###############################################


class CachePolicy(ABC):
    @abstractmethod
    def __init__(self, set, way, size):
        pass

    @abstractmethod
    def update(self, addr):
        pass

    @abstractmethod
    def evict(self, addr):
        pass

    @abstractmethod
    def contains(self, addr):
        pass


class LRUPolicy(CachePolicy):
    def __init__(self, set, way, size=4):
        self.set = set
        self.way = way
        # HowMany bits needed
        self.indexsize = int(math.log2(set))
        self.indexmask = (1 << self.indexsize) - 1
        self.size = size
        self.offsetsize = int(math.log2(size))
        # self.offsetmask = (1 << self.offsetsize) - 1
        self.cache = [OrderedDict() for _ in range(set)]

    # Whether the cache contains the block of mem
    def contains(self, addr):
        # tag | index | offset
        index = (addr >> self.offsetsize) & self.indexmask
        tag = addr >> (self.offsetsize + self.indexsize)
        try:
            if self.cache[index].get(tag):
                # contains this tag
                self.cache[index].move_to_end(tag, False)
                return True
            else:
                return False
        except IndexError:
            print(
                "Index Error at set={set}  way={way} size={size} index={index}".format(
                    set=self.set, way=self.way, size=self.size, index=self.indexsize
                )
            )
            return True

    def update(self, addr):
        index = (addr >> self.offsetsize) & self.indexmask
        tag = addr >> (self.offsetsize + self.indexsize)
        ret = True
        if (len(self.cache[index])) >= self.way:
            self.evict(addr)
            ret = False
        self.cache[index][tag] = addr
        # supposed to be data

        return ret

    def evict(self, addr):
        index = (addr >> self.offsetsize) & self.indexmask
        self.cache[index].popitem()
        # print("evicted %d" % (len(self.cache[index])))


class Cache:
    # init LRUPolicy
    def __init__(self, set, way, size):
        self.lrucache = LRUPolicy(set, way, size)
        self.hit = 0
        self.miss = 0
        self.cmiss = 0

    # check if contains otherwise update, in update auto evict block
    def access(self, addr):
        if self.lrucache.contains(addr):
            self.hit += 1
        elif self.lrucache.update(addr):
            self.cmiss += 1
        else:
            self.miss += 1

    def sim(self, path):
        file = open(path, "r")
        # print("hit:{hit}miss{miss}".format(hit=self.hit, miss=self.miss))
        for line in file:
            addr = int(line, 16)
            self.access(addr)
            # print("%x" % addr)
        print(
            "Fin: set={set},way={way},size={size}   hit={hit} miss={miss} cmiss={cmiss}".format(
                set=self.lrucache.set,
                way=self.lrucache.way,
                size=self.lrucache.size,
                hit=self.hit,
                miss=self.miss,
                cmiss=self.cmiss,
            )
        )
        return self.hit, self.miss, self.cmiss


result = []


def sim(set, way, size):
    global result
    # read the addr seq, call cache.access
    print("start sim:{set}*{way}*{size}".format(set=set, way=way, size=size))
    cac = Cache(set=set, way=way, size=size)
    hit, miss, cmiss = cac.sim("./pc_trace.txt")
    return (
        "set={set},way={way},size={size}   hit={hit} miss={miss} cmiss={cmiss}".format(
            set=set, way=way, size=size, hit=hit, miss=miss, cmiss=cmiss
        )
    )


def run_simulations():
    sim_res = []
    str_res = []
    with Pool(processes=8) as pool:
        for set in (1, 2, 4, 8, 16, 32):
            for way in (1, 2, 4, 8):
                for size in (1, 2, 4, 8, 16, 32):
                    res = pool.apply_async(func=sim, args=(set, way, 4 * size))
                    sim_res.append(res)

        # Close the pool to prevent any more tasks from being submitted
        pool.close()

        # Wait for all tasks to complete
        pool.join()
        for result in sim_res:
            print(result.get())
            str_res.append(result.get())
    log_result(str_res)
    # 等待所有线程完成
    # for thread in threads:
    #     thread.join()


def log_result(result):
    print(result)
    file = open("cache_result", "w")
    for line in result:
        file.write(line + "\n")


run_simulations()
