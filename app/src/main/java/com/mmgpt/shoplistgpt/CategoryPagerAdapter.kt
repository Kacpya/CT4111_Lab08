package com.mmgpt.shoplistgpt

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CategoryPagerAdapter(
    activity: FragmentActivity,
    private val viewModel: ShoppingListViewModel,
    private val planMode: Boolean = false
) : FragmentStateAdapter(activity) {

    private fun categoryCount(): Int = viewModel.categories.value?.size ?: 0

    override fun getItemCount(): Int = categoryCount() + 1

    /** True for the categories list page: first page in Plan mode, last in Shop mode. */
    private fun isOverviewPage(position: Int): Boolean =
        if (planMode) position == 0 else position == categoryCount()

    override fun createFragment(position: Int): Fragment =
        if (planMode) {
            if (position == 0) {
                PlanCategoriesFragment()
            } else {
                PlanCategoryFragment.newInstance(position - 1)
            }
        } else {
            if (isOverviewPage(position)) {
                ShopCategoriesFragment()
            } else {
                CategoryFragment.newInstance(position)
            }
        }

    /*
     * Fix for pages getting out of step with the list.
     *
     * By default FragmentStateAdapter uses the position as the page's ID, so
     * a fragment created for position N is reused at position N forever, even
     * if the page at N should now be a different kind of page. In Shop mode
     * the summary page's position depends on the number of categories, so
     * after an import with a different count an old summary page could sit in
     * the middle of the pager (and double-tapping that category opened a
     * summary page). After a rotation, Plan-mode fragments could also be
     * restored into this Shop-mode adapter.
     *
     * The ID now encodes the mode, the position and the page kind, so when
     * any of those change the old fragment no longer matches and is replaced.
     */
    override fun getItemId(position: Int): Long {
        val modeBit = if (planMode) PLAN_MODE_FLAG else 0L
        val kindBit = if (isOverviewPage(position)) 1L else 0L
        return modeBit or (position.toLong() shl 1) or kindBit
    }

    override fun containsItem(itemId: Long): Boolean {
        val idIsPlanMode = (itemId and PLAN_MODE_FLAG) != 0L
        if (idIsPlanMode != planMode) return false
        val position = ((itemId and PLAN_MODE_FLAG.inv()) shr 1).toInt()
        return position in 0 until itemCount && getItemId(position) == itemId
    }

    companion object {
        private const val PLAN_MODE_FLAG = 1L shl 40
    }
}
