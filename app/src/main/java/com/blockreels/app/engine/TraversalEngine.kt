package com.blockreels.app.engine

import com.blockreels.app.model.AccessibilityNode
import java.util.ArrayDeque

data class TraversalStats(
    val nodesEvaluated: Int,
    val maxDepthReached: Int,
    val durationMs: Long,
    val budgetExhausted: Boolean = false,
    val depthLimitReached: Boolean = false
)

class TraversalEngine(
    val maxDepth: Int = Int.MAX_VALUE,
    val maxBudget: Int = 150,
    private val isCurrent: () -> Boolean = { true }
) {
    private data class PendingNode(val node: AccessibilityNode, val depth: Int, val parentIndex: Int?)

    fun traverse(
        root: AccessibilityNode?,
        diagnosticVisitor: ((node: AccessibilityNode, depth: Int, index: Int, parentIndex: Int?) -> Unit)? = null,
        shouldVisitChildren: (index: Int) -> Boolean = { true },
        visitor: (node: AccessibilityNode, currentDepth: Int) -> Boolean
    ): TraversalStats {
        if (root == null) return TraversalStats(0, 0, 0L)

        val startTime = System.nanoTime()
        var nodesCount = 0
        var maxDepthReached = 0
        var budgetExhausted = false
        var depthLimitReached = false

        val queue = ArrayDeque<PendingNode>()
        queue.add(PendingNode(root, 0, null))

        while (queue.isNotEmpty() && nodesCount < maxBudget && isCurrent()) {
            val (currentNode, depth, parentIndex) = queue.poll() ?: break
            val nodeIndex = nodesCount
            nodesCount++
            if (depth > maxDepthReached) maxDepthReached = depth

            diagnosticVisitor?.invoke(currentNode, depth, nodeIndex, parentIndex)
            val shouldHalt = visitor(currentNode, depth)

            if (shouldHalt) {
                if (currentNode !== root) currentNode.recycle()
                break
            }

            if (shouldVisitChildren(nodeIndex)) {
                if (depth < maxDepth) {
                    val childCount = currentNode.childCount
                    for (i in 0 until childCount) {
                        if (!isCurrent()) break
                        if (nodesCount + queue.size >= maxBudget) {
                            budgetExhausted = true
                            break
                        }
                        val child = currentNode.getChild(i)
                        if (child != null) {
                            queue.add(PendingNode(child, depth + 1, nodeIndex))
                        }
                    }
                } else if (currentNode.childCount > 0) {
                    depthLimitReached = true
                }
            }

            if (currentNode !== root) {
                currentNode.recycle()
            }
        }

        while (queue.isNotEmpty()) {
            val (remainingNode, _) = queue.poll() ?: break
            if (remainingNode !== root) {
                remainingNode.recycle()
            }
        }

        val duration = (System.nanoTime() - startTime) / 1_000_000L
        return TraversalStats(nodesCount, maxDepthReached, duration, budgetExhausted, depthLimitReached)
    }
}
