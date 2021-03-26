/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2021 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.test.formulaeditor

import androidx.test.core.app.ApplicationProvider
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.StartScript
import org.catrobat.catroid.content.bricks.FormulaBrick
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.Functions
import org.catrobat.catroid.formulaeditor.Operators
import org.catrobat.catroid.test.utils.TestUtils
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException

@RunWith(Parameterized::class)
class FlattenListTest(
    private val name: String,
    private val formulaElementList: List<FormulaElement>,
    private val secondFormulaElementList: List<FormulaElement>,
    private val expectedValue: String?
) {
    private var projectManager: ProjectManager? = null

    companion object {
        var userList = FormulaElement(
            FormulaElement.ElementType.USER_LIST,
            "list", null
        )
        var secondUserList = FormulaElement(
            FormulaElement.ElementType.USER_LIST,
            "secondList", null
        )
        var numberOfItems = FormulaElement(
            FormulaElement.ElementType.FUNCTION,
            Functions.NUMBER_OF_ITEMS.name, null
        )
        var length = FormulaElement(
            FormulaElement.ElementType.FUNCTION,
            Functions.LENGTH.name, null
        )
        var squareRoot = FormulaElement(
            FormulaElement.ElementType.FUNCTION,
            Functions.SQRT.name, null
        )
        var userVariable = FormulaElement(
            FormulaElement.ElementType.USER_VARIABLE,
            "variable", null
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters() = listOf(
            arrayOf(
                "simple list", listOf(userList), emptyList<FormulaElement>(), "$FLATTEN( *list* ) "
            ),
            arrayOf(
                "no flatten in userlist function", listOf(numberOfItems, userList),
                emptyList<FormulaElement>(), "$NUMBER_OF_ITEMS( *list* ) "
            ),
            arrayOf(
                "user list nested once", listOf(length, userList),
                emptyList<FormulaElement>(), "$LENGTH( $FLATTEN( *list* ) ) "
            ),
            arrayOf(
                "user list nested twice", listOf(squareRoot, length, userList),
                emptyList<FormulaElement>(), "$SQRT( $LENGTH( $FLATTEN( *list* ) ) ) "
            ),
            arrayOf(
                "user variable", listOf(userVariable), emptyList<FormulaElement>(), "\"variable\" "
            ),
            arrayOf(
                "user variable nested once",
                listOf(squareRoot, userVariable),
                emptyList<FormulaElement>(),
                "$SQRT( \"variable\" ) "
            ),
            arrayOf(
                "multiple user lists", listOf(userList), listOf(secondUserList),
                "$FLATTEN( *list* ) $ADDITION $FLATTEN( *secondList* ) "
            ),
            arrayOf(
                "multiple nested user lists", listOf(length, userList),
                listOf(squareRoot, secondUserList),
                "$LENGTH( $FLATTEN( *list* ) ) $ADDITION $SQRT( $FLATTEN( *secondList* ) ) "
            )
        )

        private const val COLLISION_TEST_PROJECT = "COLLISION_TEST_PROJECT"
        private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        private val FLATTEN = context.getString(R.string.formula_editor_function_flatten)
        private val NUMBER_OF_ITEMS = context.getString(R.string.formula_editor_function_number_of_items)
        private val LENGTH = context.getString(R.string.formula_editor_function_length)
        private val SQRT = context.getString(R.string.formula_editor_function_sqrt)
        private val ADDITION = context.getString(R.string.formula_editor_operator_plus)
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        projectManager = ProjectManager.getInstance()
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        projectManager?.currentProject = null
        TestUtils.deleteProjects(COLLISION_TEST_PROJECT)
    }

    @Test
    @Throws(IOException::class)
    fun testFlattenAllLists() {
        val project = createProjectWithFormulaElements()
        ProjectManager.flattenAllLists(project)
        val sprite = project.defaultScene?.getSprite("Sprite")
        val brick = sprite?.getScript(0)?.getBrick(0)
        Assert.assertThat(brick, Matchers.`is`(Matchers.instanceOf(FormulaBrick::class.java)))
        val formulaBrick = brick as FormulaBrick
        val newFormula =
            formulaBrick.formulas[0].getTrimmedFormulaString(ApplicationProvider.getApplicationContext())
        val expected = expectedValue
        junit.framework.Assert.assertEquals(expected, newFormula)
        TestUtils.deleteProjects()
    }

    private fun createProjectWithFormulaElements(): Project {
        val project = Project(ApplicationProvider.getApplicationContext(), "Project")
        val sprite = Sprite("Sprite")
        val script = StartScript()

        setAllFormulaElementChildren(formulaElementList)
        setAllFormulaElementChildren(secondFormulaElementList)

        val formula = if (secondFormulaElementList.isNotEmpty()) {
            val additionFormulaElement = FormulaElement(
                FormulaElement.ElementType.OPERATOR,
                Operators.PLUS.name, null, formulaElementList[0], secondFormulaElementList[0]
            )
            Formula(additionFormulaElement)
        } else {
            Formula(formulaElementList[0])
        }

        val ifBrick = IfLogicBeginBrick(formula)
        script.addBrick(ifBrick)
        sprite.addScript(script)
        project.defaultScene.addSprite(sprite)
        val projectManager = ProjectManager.getInstance()
        projectManager.currentProject = project
        return project
    }

    private fun setAllFormulaElementChildren(formulaElementList: List<FormulaElement>): FormulaElement? {
        var previousElement: FormulaElement? = null
        for (formulaElement in formulaElementList) {
            previousElement?.setLeftChild(formulaElement)
            previousElement = formulaElement
        }
        return previousElement
    }
}
