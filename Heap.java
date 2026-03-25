/**
 * Heap
 *
 * An implementation of Fibonacci heap over positive integers
 * with the possibility of not performing lazy melds and
 * the possibility of not performing lazy decrease keys.
 */
public class Heap {

    public final boolean lazyMelds;
    public final boolean lazyDecreaseKeys;
    public HeapItem min; // The minimum item

    private HeapNode firstRoot;
    private int size;
    private int numTrees;
    private int numMarkedNodes;
    private int totalLinks;
    private int totalCuts;
    private int totalHeapifyCosts;

    /**
     * Constructor to initialize an empty heap.
     */
    public Heap(boolean lazyMelds, boolean lazyDecreaseKeys) {
        // Store configuration flags
        this.lazyMelds = lazyMelds;
        this.lazyDecreaseKeys = lazyDecreaseKeys;

        // Initialize empty heap structure
        this.min = null;
        this.firstRoot = null;
        this.size = 0;
        this.numTrees = 0;
        this.numMarkedNodes = 0;

        // Initialize statistics counters
        this.totalLinks = 0;
        this.totalCuts = 0;
        this.totalHeapifyCosts = 0;
    }

    /**
     * Insert (key,info) into the heap.
     */
    public HeapItem insert(int key, String info) {
        // Create logical item
        HeapItem item = new HeapItem(key, info);

        // Create structural node
        HeapNode node = new HeapNode(item);
        item.node = node;

        // Initialize node as a circular list of size 1
        node.next = node;
        node.prev = node;

        // Create a temporary heap containing only this node
        Heap temp = new Heap(lazyMelds, lazyDecreaseKeys);
        temp.firstRoot = node;
        temp.min = item;
        temp.size = 1;
        temp.numTrees = 1;

        // Merge temporary heap into current heap
        meld(temp);

        // Return pointer to logical item
        return item;
    }

    public HeapItem findMin() {
        // Direct access to minimum pointer
        return min;
    }

    /**
     * Delete the minimal item.
     */
    public void deleteMin() {
        // Nothing to delete
        if (min == null)
            return;

        // Get structural node of minimum
        HeapNode z = min.node;

        // Remove z from root list
        if (z.next == z) {
            firstRoot = null;
        } else {
            z.prev.next = z.next;
            z.next.prev = z.prev;
            if (firstRoot == z)
                firstRoot = z.next;
        }
        numTrees--;

        // Promote children of z to root list
        if (z.child != null) {
            HeapNode c = z.child;
            HeapNode curr = c;

            // Detach children from parent and clear marks
            do {
                curr.parent = null;
                if (curr.marked) {
                    curr.marked = false;
                    numMarkedNodes--;
                }
                curr = curr.next;
            } while (curr != c);

            // Build temporary heap from children
            Heap temp = new Heap(lazyMelds, lazyDecreaseKeys);
            temp.firstRoot = c;
            temp.numTrees = z.rank;

            // Find minimum among children
            HeapNode it = c;
            HeapItem childMin = c.item;
            do {
                if (it.item.key < childMin.key)
                    childMin = it.item;
                it = it.next;
            } while (it != c);
            temp.min = childMin;

            // Concatenate children into root list
            linkListsOnly(temp);
        }

        size--;

        // If heap becomes empty, reset all fields
        if (size == 0) {
            min = null;
            firstRoot = null;
            numTrees = 0;
            numMarkedNodes = 0;
        } else {
            // Consolidate roots
            successiveLinking();
        }
    }

    /**
     * Helper to join circular lists without logic
     */
    private void linkListsOnly(Heap heap2) {
        // Nothing to link
        if (heap2 == null || heap2.firstRoot == null)
            return;

        // If current heap is empty, adopt heap2
        if (firstRoot == null) {
            firstRoot = heap2.firstRoot;
            min = heap2.min;
            numTrees = heap2.numTrees;
        } else {
            // Concatenate two circular doubly-linked lists
            HeapNode aLast = firstRoot.prev;
            HeapNode bFirst = heap2.firstRoot;
            HeapNode bLast = bFirst.prev;

            aLast.next = bFirst;
            bFirst.prev = aLast;
            bLast.next = firstRoot;
            firstRoot.prev = bLast;

            // Update minimum if needed
            if (heap2.min.key < min.key)
                min = heap2.min;

            numTrees += heap2.numTrees;
        }
    }

    /**
     * Decrease key of x.
     */
    public void decreaseKey(HeapItem x, int diff) {
        // Invalid input
        if (x == null || diff == 0)
            return;

        // Decrease logical key
        x.key -= diff;
        HeapNode node = x.node;

        // Non-lazy mode: restore order by bubbling up
        if (!lazyDecreaseKeys) {
            heapifyUp(node);
            return;
        }

        // Lazy mode: check heap-order violation
        HeapNode y = node.parent;
        if (y != null && node.item.key < y.item.key) {
            cut(node, y);
            cascadingCut(y);
        }

        // Update global minimum if needed
        if (x.key < min.key)
            min = x;
    }

    private void cascadingCut(HeapNode y) {
        // Get parent
        HeapNode z = y.parent;

        if (z != null) {
            // First child loss: mark
            if (!y.marked) {
                y.marked = true;
                numMarkedNodes++;
            } else {
                // Second child loss: cut and recurse upward
                cut(y, z);
                cascadingCut(z);
            }
        }
    }

    private void cut(HeapNode x, HeapNode parent) {
        // Remove x from parent's child list
        if (x.next == x)
            parent.child = null;
        else {
            x.prev.next = x.next;
            x.next.prev = x.prev;
            if (parent.child == x)
                parent.child = x.next;
        }

        // Update parent rank and detach x
        parent.rank--;
        x.parent = null;

        // Clear mark if needed
        if (x.marked) {
            x.marked = false;
            numMarkedNodes--;
        }

        // Make x a standalone root
        x.next = x;
        x.prev = x;

        // Insert x into root list via meld
        Heap temp = new Heap(lazyMelds, lazyDecreaseKeys);
        temp.firstRoot = x;
        temp.min = x.item;
        temp.numTrees = 1;

        meld(temp);
        totalCuts++;
    }

    private void heapifyUp(HeapNode x) {
        // Bubble item up while heap order is violated
        while (x.parent != null && x.item.key < x.parent.item.key) {
            HeapItem tmp = x.item;
            x.item = x.parent.item;
            x.item.node = x;
            x.parent.item = tmp;
            x.parent.item.node = x.parent;
            x = x.parent;
            totalHeapifyCosts++;
        }

        // Update minimum if needed
        if (x.item.key < min.key)
            min = x.item;
    }

    public void delete(HeapItem x) {
        // Nothing to delete
        if (x == null)
            return;

        // Force x to become minimum
        int diff = x.key - Integer.MIN_VALUE;
        decreaseKey(x, diff);

        // Remove minimum
        deleteMin();
    }

    public void meld(Heap heap2) {
        // Nothing to meld
        if (heap2 == null || heap2.firstRoot == null)
            return;

        // Merge statistics
        addToCounters(heap2);

        // Update size and concatenate root lists
        size += heap2.size;
        linkListsOnly(heap2);

        // Perform consolidation if not lazy
        if (!lazyMelds)
            successiveLinking();
    }

    public void successiveLinking() {
        // Empty heap
        if (firstRoot == null)
            return;

        // Consolidate roots by rank
        HeapNode[] boxs = consolidate();

        // Rebuild root list from consolidated trees
        rebuildRootList(boxs);

        // Clear marks if decreaseKey is not lazy
        if (!lazyDecreaseKeys) {
            numMarkedNodes = 0;
        }
    }

    private HeapNode[] consolidate() {
        // Compute maximum possible rank
        int maxRank = (size <= 1) ? 2 : (32 - Integer.numberOfLeadingZeros(size - 1)) + 2;
        HeapNode[] boxs = new HeapNode[maxRank];

        // Copy roots to temporary array
        HeapNode[] roots = new HeapNode[numTrees];
        HeapNode curr = firstRoot;
        for (int i = 0; i < roots.length; i++) {
            roots[i] = curr;
            curr = curr.next;
        }

        // Process each root
        for (HeapNode w : roots) {
            // Detach w as a standalone tree
            w.next = w;
            w.prev = w;
            w.parent = null;

            // Root cannot be marked
            if (w.marked) {
                w.marked = false;
                numMarkedNodes--;
            }

            // Merge trees of equal rank
            int d = w.rank;
            while (boxs[d] != null) {
                w = link(w, boxs[d]);
                boxs[d] = null;
                d = w.rank;
            }
            boxs[d] = w;
        }

        return boxs;
    }

    private void rebuildRootList(HeapNode[] boxs) {
        // Reset root list
        firstRoot = null;
        min = null;
        numTrees = 0;

        // Reinsert all roots from array
        for (HeapNode x : boxs) {
            if (x != null) {
                if (firstRoot == null) {
                    firstRoot = x;
                    x.next = x;
                    x.prev = x;
                    min = x.item;
                } else {
                    HeapNode last = firstRoot.prev;
                    last.next = x;
                    x.prev = last;
                    x.next = firstRoot;
                    firstRoot.prev = x;
                    if (x.item.key < min.key)
                        min = x.item;
                }
                numTrees++;
            }
        }
    }

    private void addToCounters(Heap heap2) {
        // Aggregate statistics counters
        totalLinks += heap2.totalLinks;
        totalCuts += heap2.totalCuts;
        totalHeapifyCosts += heap2.totalHeapifyCosts;
        numMarkedNodes += heap2.numMarkedNodes;
    }

    private HeapNode link(HeapNode a, HeapNode b) {
        // Ensure a has smaller key
        if (b.item.key < a.item.key) {
            HeapNode tmp = a;
            a = b;
            b = tmp;
        }

        // Make b a child of a
        b.parent = a;
        if (a.child == null) {
            a.child = b;
            b.next = b;
            b.prev = b;
        } else {
            HeapNode c = a.child;
            HeapNode last = c.prev;
            last.next = b;
            b.prev = last;
            b.next = c;
            c.prev = b;
        }

        a.rank++;
        totalLinks++;
        return a;
    }

    // Getters for statistics
    public int size() {
        return size;
    }

    public int numTrees() {
        return numTrees;
    }

    public int numMarkedNodes() {
        return numMarkedNodes;
    }

    public int totalLinks() {
        return totalLinks;
    }

    public int totalCuts() {
        return totalCuts;
    }

    public int totalHeapifyCosts() {
        return totalHeapifyCosts;
    }

    public static class HeapNode {
        public HeapItem item;
        public HeapNode child, next, prev, parent;
        public int rank;
        public boolean marked;

        public HeapNode(HeapItem item) {
            this.item = item;
        }
    }

    public static class HeapItem {
        public HeapNode node;
        public int key;
        public String info;

        public HeapItem(int key, String info) {
            this.key = key;
            this.info = info;
        }
    }
}
