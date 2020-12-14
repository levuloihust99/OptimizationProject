import argparse
from utils import read_input, random_chunk, clone
from tqdm import tqdm
import random


parser = argparse.ArgumentParser()
parser.add_argument("--input", type=str, default='data/VRP-N006-K002/B100.ins')
args = parser.parse_args()

(N, K), D, T = read_input(args.input)


class GA(object):
    def __init__(self, population_size=100, mutation_rate=0.05, crossover_rate=0.2, n_epochs=200):
        self.population_size = population_size
        self.mutation_rate = mutation_rate
        self.crossover_rate = crossover_rate
        self.population = None
        self.n_epochs = n_epochs
    
    def initizlize_population(self):
        self.population = []
        for i in tqdm(range(self.population_size)):
            s = list(range(1, N + 1))
            candidate = random_chunk(s, K)
            self.population.append(
                (candidate, fitness(candidate))
            )

    def evolution(self):
        for i in range(self.n_epochs):
            print(f"EPOCH: {i}")
            new_population = self.population
            s = list(range(0, self.population_size))
            random.shuffle(s)
            mutations_indice = s[0:int(self.mutation_rate * self.population_size)]
            print("mutating....")
            for index in tqdm(mutations_indice):
                s1 = mutate_inside_path(self.population[index][0])
                new_population.append(
                    (s1, fitness(s1))
                )
                s2 = mutate_outside_path(self.population[index][0])
                new_population.append(
                    (s2, fitness(s2))
                )
            s1 = list(range(0, self.population_size))
            random.shuffle(s1)
            mothers_indice = s1[0:int(self.crossover_rate * self.population_size)]
            s2 = list(range(0, self.population_size))
            random.shuffle(s2)
            farthers_indice = s2[0:int(self.crossover_rate * self.population_size)]
            
            print("crossover....")
            for i in range(len(mothers_indice)):
                mother = self.population[mothers_indice[i]][0]
                father = self.population[farthers_indice[i]][0]
                child1, child2 = crossover(mother, father)
                new_population.append(
                    (child1, fitness(child1))
                )
                new_population.append(
                    (child2, fitness(child2))
                )

            new_population.sort(key=lambda tup: tup[1])
            self.population = new_population[0:self.population_size]
            print("Top 10 best candidates:")
            for i in range(10):
                print(f"{self.population[i]}: {self.population[i][1]}")


def fitness(candidate: list) -> int:
    path_costs = []
    for path in candidate:
        if len(path) == 0:
            path_costs.append(0)
        else:
            s = 0
            s += T[0][path[0]]
            for i in range(len(path) - 1):
                s += T[path[i]][path[i + 1]]
            s += T[path[-1]][0]
            for customer in path:
                s += D[customer]
            path_costs.append(s)
    return max(path_costs)


def crossover(s1: list, s2: list):
    mother = clone(s1)
    father = clone(s2)
    assert len(mother) == K
    assert len(father) == K

    gens_len = random.randint(1, 3)

    path_id1 = random.randint(0, K - 1)
    while len(mother[path_id1]) < gens_len:
        path_id1 = random.randint(0, K - 1)
    mother_start = random.randint(0, len(mother[path_id1]) - gens_len)

    path_id2 = random.randint(0, K - 1)
    while len(father[path_id2]) < gens_len:
        path_id2 = random.randint(0, K - 1)
    father_start = random.randint(0, len(father[path_id2]) - gens_len)

    mother_gens = mother[path_id1][mother_start: mother_start + gens_len]
    father_gens = father[path_id2][father_start: father_start + gens_len]

    child1 = clone(s1)
    child2 = clone(s2)

    child1[path_id1] = mother[path_id1][0:mother_start] + father_gens + mother[path_id1][mother_start + gens_len:]
    child2[path_id2] = father[path_id2][0:father_start] + mother_gens + father[path_id2][father_start + gens_len:]
    
    s = set()
    for i in mother_gens + father_gens:
        if i in mother_gens and i in father_gens:
            s.add(i)
    
    left = []
    right = []

    for i in mother_gens:
        if i not in s:
            left.append(i)

    for i in father_gens:
        if i not in s:
            right.append(i)
    random.shuffle(left)
    random.shuffle(right)

    mapping_left = {}
    mapping_right = {}
    assert len(left) == len(right)
    for i in range(len(left)):
        mapping_left[right[i]] = left[i]
        mapping_right[left[i]] = right[i]

    for i in range(len(child1)):
        if i == path_id1:
            for j in range(len(child1[i])):
                if j not in range(mother_start, mother_start + gens_len):
                    if child1[i][j] in mapping_left.keys():
                        child1[i][j] = mapping_left[child1[i][j]]
        else:
            for j in range(len(child1[i])):
                if child1[i][j] in mapping_left.keys():
                    child1[i][j] = mapping_left[child1[i][j]]

    for i in range(len(child2)):
        if i == path_id2:
            for j in range(len(child2[i])):
                if j not in range(father_start, father_start + gens_len):
                    if child2[i][j] in mapping_right.keys():
                        child2[i][j] = mapping_right[child2[i][j]]
        else:
            for j in range(len(child2[i])):
                if child2[i][j] in mapping_right.keys():
                    child2[i][j] = mapping_right[child2[i][j]]
    return child1, child2


def mutate_inside_path(root: list):
    root = clone(root)
    assert len(root) == K
    path_id = random.randint(0, K - 1)
    while len(root[path_id]) < 2:
        path_id = random.randint(0, K - 1)
    index = random.randint(0, len(root[path_id]) - 2)

    new_candidate = clone(root)
    cache = new_candidate[path_id][index + 1]
    new_candidate[path_id][index + 1] = new_candidate[path_id][index]
    new_candidate[path_id][index] = cache
    return new_candidate


def mutate_outside_path(root: list):
    root = clone(root)
    assert len(root) == K

    gens_len = random.randint(1, 2)
    path_id = random.randint(0, K - 1)
    while len(root[path_id]) < gens_len:
        path_id = random.randint(0, K - 1)

    start = random.randint(0, len(root[path_id]) - gens_len)
    gens = root[path_id][start:start + gens_len]

    path_id2 = random.randint(0, K - 1)
    while path_id2 == path_id:
        path_id2 = random.randint(0, K - 1)

    new_candidate = clone(root)
    if(len(new_candidate[path_id2])) == 0:
        index = 0
    else:
        index = random.randint(0, len(new_candidate[path_id2]) - 1)
    new_candidate[path_id2] = new_candidate[path_id2][0:index] + gens + new_candidate[path_id2][index:]
    new_candidate[path_id] = root[path_id][0:start] + root[path_id][start + gens_len:]
    return new_candidate

ga = GA()
ga.initizlize_population()
ga.evolution()
