package dev.heizer.core.btree

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class BTree<T : Comparable<T>> {
    var root: Node<T>? = null

    @Serializable
    class Node<T>(
        val id: Long,
        var value: T,
        var left: Node<T>? = null,
        var right: Node<T>? = null
    ) {
    }

    fun insert(value: T) {
        root = insertRecursive(root, value)
    }

    private fun insertRecursive(current: Node<T>?, value: T): Node<T> {
        if (current == null) {
            return Node(UUID.randomUUID().mostSignificantBits, value)
        }

        if (value < current.value) {
            current.left = insertRecursive(current.left, value)
        } else if (value > current.value) {
            current.right = insertRecursive(current.right, value)
        }

        return current
    }

    fun find(value: T): Node<T>? {
        return findRecursive(root, value)
    }

    private fun findRecursive(current: Node<T>?, id: Long): Node<T>? {
        if (current == null || current.id == id) {
            return current
        }

        return if (id < current.id) {
            findRecursive(current.left, id)
        } else {
            findRecursive(current.right, id)
        }
    }

    private fun findRecursive(current: Node<T>?, value: T): Node<T>? {
        if (current == null || current.value == value) {
            return current
        }

        return if (value < current.value) {
            findRecursive(current.left, value)
        } else {
            findRecursive(current.right, value)
        }
    }

    fun delete(value: T) {
        root = deleteRecursive(root, value)
    }

    private fun deleteRecursive(current: Node<T>?, value: T): Node<T>? {
        if (current == null) {
            return null
        }

        if (value == current.value) {
            if (current.left == null && current.right == null) {
                return null
            }

            if (current.right == null) {
                return current.left
            }
            if (current.left == null) {
                return current.right
            }

            val smallestValue = findSmallestValue(current.right!!)
            current.value = smallestValue
            current.right = deleteRecursive(current.right, smallestValue)
            return current
        }

        if (value < current.value) {
            current.left = deleteRecursive(current.left, value)
            return current
        }

        current.right = deleteRecursive(current.right, value)
        return current
    }

    private fun findSmallestValue(root: Node<T>): T {
        return root.left?.let { findSmallestValue(it) } ?: root.value
    }

    fun traverseInOrder(visitor: (T) -> Unit) {
        traverseInOrderRecursive(root, visitor)
    }

    private fun traverseInOrderRecursive(node: Node<T>?, visitor: (T) -> Unit) {
        if (node != null) {
            traverseInOrderRecursive(node.left, visitor)
            visitor(node.value)
            traverseInOrderRecursive(node.right, visitor)
        }
    }

    fun traversePreOrder(visitor: (T) -> Unit) {
        traversePreOrderRecursive(root, visitor)
    }

    private fun traversePreOrderRecursive(node: Node<T>?, visitor: (T) -> Unit) {
        if (node != null) {
            visitor(node.value)
            traversePreOrderRecursive(node.left, visitor)
            traversePreOrderRecursive(node.right, visitor)
        }
    }

    fun traversePostOrder(visitor: (T) -> Unit) {
        traversePostOrderRecursive(root, visitor)
    }

    private fun traversePostOrderRecursive(node: Node<T>?, visitor: (T) -> Unit) {
        if (node != null) {
            traversePostOrderRecursive(node.left, visitor)
            traversePostOrderRecursive(node.right, visitor)
            visitor(node.value)
        }
    }

    fun traverseLevelOrder(visitor: (T) -> Unit) {
        val queue = mutableListOf<Node<T>>()
        root?.let { queue.add(it) }

        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)
            visitor(node.value)
            node.left?.let { queue.add(it) }
            node.right?.let { queue.add(it) }
        }
    }

    override fun toString(): String {
        return buildString {
            appendTreeStructure(root, "", true)
        }
    }

    private fun StringBuilder.appendTreeStructure(node: Node<T>?, prefix: String, isTail: Boolean) {
        if (node != null) {
            append(prefix + (if (isTail) "└── " else "├── ") + node.value + "\n")
            val nextPrefix = prefix + (if (isTail) "    " else "│   ")
            val children = listOfNotNull(node.left, node.right)
            for (i in 0 until children.size) {
                appendTreeStructure(children[i], nextPrefix, i == children.size - 1)
            }
        }
    }

}