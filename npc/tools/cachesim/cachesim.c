#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <assert.h>

// Cache configuration
typedef struct {
    uint32_t size;          // Total size in bytes
    uint32_t block_size;    // Block size in bytes
    uint32_t assoc;         // Associativity (ways)
} CacheConfig;

// Cache Line Metadata
typedef struct {
    bool valid;
    uint32_t tag;
    uint32_t last_access_time; // For LRU replacement
} CacheLine;

// Cache Set
typedef struct {
    CacheLine *lines;
} CacheSet;

// Cache Simulator State
typedef struct {
    CacheConfig config;
    CacheSet *sets;
    uint32_t num_sets;
    uint32_t index_mask;
    uint32_t index_bits;
    uint32_t block_bits;
    uint32_t tag_mask;
    
    // Statistics
    uint64_t accesses;
    uint64_t hits;
    uint64_t misses;
    uint64_t current_time;
} CacheSim;

// Initialize Cache Simulator
CacheSim* cachesim_init(uint32_t size, uint32_t block_size, uint32_t assoc) {
    CacheSim *sim = (CacheSim*)malloc(sizeof(CacheSim));
    sim->config.size = size;
    sim->config.block_size = block_size;
    sim->config.assoc = assoc;
    
    sim->num_sets = size / (block_size * assoc);
    sim->sets = (CacheSet*)malloc(sizeof(CacheSet) * sim->num_sets);
    
    for (uint32_t i = 0; i < sim->num_sets; i++) {
        sim->sets[i].lines = (CacheLine*)malloc(sizeof(CacheLine) * assoc);
        for (uint32_t j = 0; j < assoc; j++) {
            sim->sets[i].lines[j].valid = false;
            sim->sets[i].lines[j].tag = 0;
            sim->sets[i].lines[j].last_access_time = 0;
        }
    }
    
    // Calculate bit masks and shifts
    sim->block_bits = 0;
    while ((1 << sim->block_bits) < block_size) sim->block_bits++;
    
    sim->index_bits = 0;
    while ((1 << sim->index_bits) < sim->num_sets) sim->index_bits++;
    
    sim->index_mask = (1 << sim->index_bits) - 1;
    
    sim->accesses = 0;
    sim->hits = 0;
    sim->misses = 0;
    sim->current_time = 0;
    
    printf("CacheSim Init: Size=%d, Block=%d, Assoc=%d, Sets=%d\n", 
           size, block_size, assoc, sim->num_sets);
    
    return sim;
}

// Access Cache
void cachesim_access(CacheSim *sim, uint32_t addr) {
    sim->current_time++;
    sim->accesses++;
    
    uint32_t index = (addr >> sim->block_bits) & sim->index_mask;
    uint32_t tag = addr >> (sim->block_bits + sim->index_bits);
    
    CacheSet *set = &sim->sets[index];
    int hit_way = -1;
    
    // Check for hit
    for (uint32_t i = 0; i < sim->config.assoc; i++) {
        if (set->lines[i].valid && set->lines[i].tag == tag) {
            hit_way = i;
            break;
        }
    }
    
    if (hit_way != -1) {
        sim->hits++;
        set->lines[hit_way].last_access_time = sim->current_time;
    } else {
        sim->misses++;
        
        // Find victim (LRU)
        int victim_way = -1;
        uint32_t min_time = UINT32_MAX;
        
        // First look for invalid lines
        for (uint32_t i = 0; i < sim->config.assoc; i++) {
            if (!set->lines[i].valid) {
                victim_way = i;
                break;
            }
        }
        
        // If no invalid lines, find LRU
        if (victim_way == -1) {
            for (uint32_t i = 0; i < sim->config.assoc; i++) {
                if (set->lines[i].last_access_time < min_time) {
                    min_time = set->lines[i].last_access_time;
                    victim_way = i;
                }
            }
        }
        
        // Replace
        set->lines[victim_way].valid = true;
        set->lines[victim_way].tag = tag;
        set->lines[victim_way].last_access_time = sim->current_time;
    }
}

void cachesim_print_stats(CacheSim *sim) {
    printf("Accesses: %lu\n", sim->accesses);
    printf("Hits:     %lu (%.2f%%)\n", sim->hits, 100.0 * sim->hits / sim->accesses);
    printf("Misses:   %lu (%.2f%%)\n", sim->misses, 100.0 * sim->misses / sim->accesses);
}

int main(int argc, char *argv[]) {
    if (argc < 2) {
        printf("Usage: %s <log_file> [size] [block_size] [assoc]\n", argv[0]);
        return 1;
    }
    
    char *log_file = argv[1];
    uint32_t size = (argc > 2) ? atoi(argv[2]) : 4096;
    uint32_t block_size = (argc > 3) ? atoi(argv[3]) : 16; // Default 16B block
    uint32_t assoc = (argc > 4) ? atoi(argv[4]) : 1;       // Default Direct Mapped
    
    CacheSim *sim = cachesim_init(size, block_size, assoc);
    
    FILE *fp = fopen(log_file, "r");
    if (!fp) {
        perror("Failed to open log file");
        return 1;
    }
    
    char line[256];
    uint32_t pc;
    // Assuming log format is just PC in hex per line, or standard itrace format
    // We need to parse PC from the log. 
    // Let's assume a simple format for now: "0x80000000" or just "80000000"
    // Or standard NEMU itrace: "0x80000000: ..."
    
    while (fgets(line, sizeof(line), fp)) {
        // Simple parsing logic: look for "0x" and read hex
        char *pc_str = strstr(line, "0x");
        if (pc_str) {
            pc = strtoul(pc_str, NULL, 16);
            // Filter out 0 or very low addresses if necessary
            if (pc >= 0x10000000) { 
                cachesim_access(sim, pc);
            }
        }
    }
    
    cachesim_print_stats(sim);
    
    fclose(fp);
    return 0;
}
