# Fibonacci Heap — Advanced Priority Queue in Java

Implementation of a configurable Fibonacci heap as part of the Data Structures course (Project 2) at Tel Aviv University.

## Overview

A Fibonacci heap is a priority queue with excellent amortized performance. This implementation goes beyond the standard Fibonacci heap — it supports **four distinct heap behaviors** controlled by two boolean flags, allowing comparison between binomial heaps, lazy binomial heaps, and Fibonacci heaps within a single implementation.

## Configurable Modes

The heap behavior is determined at construction by two boolean flags:

| `lazyMelds` | `lazyDecreaseKeys` | Behavior |
|---|---|---|
| `false` | `false` | Binomial Heap |
| `true` | `false` | Lazy Binomial Heap |
| `true` | `true` | Fibonacci Heap |
| `false` | `true` | Binomial Heap with Cascading Cuts |

```java
Heap fibonacci  = new Heap(true, true);   // Fibonacci Heap
Heap binomial   = new Heap(false, false); // Binomial Heap
Heap lazyBinom  = new Heap(true, false);  // Lazy Binomial Heap
Heap withCuts   = new Heap(false, true);  // Binomial Heap with cuts
```

## Implemented Operations

| Operation | Description |
|---|---|
| `insert(k, info)` | Insert a new item, returns a pointer to the created `HeapItem` |
| `findMin()` | Returns the minimum item in O(1) |
| `deleteMin()` | Removes the minimum and consolidates via successive linking |
| `decreaseKey(x, d)` | Decreases key of item `x` by `d`; uses cascading cut or heapifyUp depending on mode |
| `delete(x)` | Deletes any item by decreasing its key to −∞ then calling `deleteMin` |
| `meld(heap2)` | Merges two heaps; runs successive linking if `lazyMelds=false` |

## Statistics Tracking

The heap tracks internal counters for performance analysis and amortized experiments:

```java
heap.totalLinks()         // total link operations (merging two equal-rank trees)
heap.totalCuts()          // total cut operations (cascading cuts in decreaseKey/delete)
heap.totalHeapifyCosts()  // total steps of heapifyUp (non-lazy decrease key mode)
heap.numTrees()           // current number of trees in the root list
heap.numMarkedNodes()     // current number of marked nodes
heap.size()               // number of items in the heap
```

## Amortized Complexity

| Operation | Binomial | Lazy Binomial | Fibonacci |
|---|---|---|---|
| `insert` | O(log n) | O(1) | O(1) |
| `findMin` | O(1) | O(1) | O(1) |
| `deleteMin` | O(log n) | O(log n) | O(log n) |
| `decreaseKey` | O(log n) | O(log n) | O(1) |
| `delete` | O(log n) | O(log n) | O(log n) |

## Usage

```java
Heap h = new Heap(true, true); // Fibonacci Heap

HeapItem a = h.insert(10, "ten");
HeapItem b = h.insert(3, "three");
HeapItem c = h.insert(7, "seven");

System.out.println(h.findMin().key);  // 3

h.decreaseKey(a, 8);  // key: 10 → 2
System.out.println(h.findMin().key);  // 2

h.deleteMin();
System.out.println(h.findMin().key);  // 3

System.out.println(h.totalLinks());   // number of link operations performed
```

## Academic Context

Implemented as part of **Project 2 — Advanced Heaps** in the Data Structures course at Tel Aviv University (Semester A, 2025–2026).
