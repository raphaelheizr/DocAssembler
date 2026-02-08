package dev.heizer.infrastructure.btree

import dev.heizer.core.btree.BTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BTreeTest {

    @Test
    fun testInsertionAndTraversal() {
        val tree = BTree<Int>()
        tree.insert(5)
        tree.insert(3)
        tree.insert(7)
        tree.insert(2)
        tree.insert(4)
        tree.insert(6)
        tree.insert(8)

        val inOrderResult = mutableListOf<Int>()
        tree.traverseInOrder { inOrderResult.add(it) }
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8), inOrderResult)

        val preOrderResult = mutableListOf<Int>()
        tree.traversePreOrder { preOrderResult.add(it) }
        assertEquals(listOf(5, 3, 2, 4, 7, 6, 8), preOrderResult)

        val postOrderResult = mutableListOf<Int>()
        tree.traversePostOrder { postOrderResult.add(it) }
        assertEquals(listOf(2, 4, 3, 6, 8, 7, 5), postOrderResult)

        val levelOrderResult = mutableListOf<Int>()
        tree.traverseLevelOrder { levelOrderResult.add(it) }
        assertEquals(listOf(5, 3, 7, 2, 4, 6, 8), levelOrderResult)
    }

    @Test
    fun testFind() {
        val tree = BTree<Int>()
        tree.insert(5)
        tree.insert(3)
        tree.insert(7)

        assertNotNull(tree.find(5))
        assertNotNull(tree.find(3))
        assertNotNull(tree.find(7))
        assertNull(tree.find(10))
    }

    @Test
    fun testDelete() {
        val tree = BTree<Int>()
        tree.insert(5)
        tree.insert(3)
        tree.insert(7)
        tree.insert(2)
        tree.insert(4)
        tree.insert(6)
        tree.insert(8)

        // Delete leaf
        tree.delete(2)
        val res1 = mutableListOf<Int>()
        tree.traverseInOrder { res1.add(it) }
        assertEquals(listOf(3, 4, 5, 6, 7, 8), res1)

        // Delete node with one child
        tree.delete(3)
        val res2 = mutableListOf<Int>()
        tree.traverseInOrder { res2.add(it) }
        assertEquals(listOf(4, 5, 6, 7, 8), res2)

        // Delete node with two children
        tree.delete(7)
        val res3 = mutableListOf<Int>()
        tree.traverseInOrder { res3.add(it) }
        assertEquals(listOf(4, 5, 6, 8), res3)
        
        // Delete root
        tree.delete(5)
        val res4 = mutableListOf<Int>()
        tree.traverseInOrder { res4.add(it) }
        assertEquals(listOf(4, 6, 8), res4)
    }

    @Test
    fun testToString() {
        val tree = BTree<Int>()
        tree.insert(5)
        tree.insert(3)
        tree.insert(7)
        
        val expected = "└── 5\n    ├── 3\n    └── 7\n"
        assertEquals(expected, tree.toString())
    }
}
