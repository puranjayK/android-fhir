/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.datacapture.validation

import android.content.Context
import com.google.android.fhir.datacapture.hasNestedItemsWithinAnswers
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

object QuestionnaireResponseValidator {

  /** Maps linkId to [ValidationResult]. */
  private val linkIdToValidationResultMap = mutableMapOf<String, MutableList<ValidationResult>>()

  /**
   * Validates [questionnaireResponseItemList] using the constraints defined in the
   * [questionnaireItemList].
   */
  fun validateQuestionnaireResponseAnswers(
    questionnaireItemList: List<Questionnaire.QuestionnaireItemComponent>,
    questionnaireResponseItemList: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
    context: Context
  ): Map<String, List<ValidationResult>> {
    /* TODO create an iterator for questionnaire item + questionnaire response item refer to the
    questionnaire view model */
    val questionnaireItemListIterator = questionnaireItemList.iterator()
    val questionnaireResponseItemListIterator = questionnaireResponseItemList.iterator()
    while (questionnaireItemListIterator.hasNext() &&
      questionnaireResponseItemListIterator.hasNext()) {
      val questionnaireItem = questionnaireItemListIterator.next()
      val questionnaireResponseItem = questionnaireResponseItemListIterator.next()
      linkIdToValidationResultMap[questionnaireItem.linkId] = mutableListOf()
      linkIdToValidationResultMap[questionnaireItem.linkId]?.add(
        QuestionnaireResponseItemValidator.validate(
          questionnaireItem,
          questionnaireResponseItem,
          context
        )
      )
      if (questionnaireItem.hasNestedItemsWithinAnswers) {
        // TODO(https://github.com/google/android-fhir/issues/487): Validates all answers.
        validateQuestionnaireResponseAnswers(
          questionnaireItem.item,
          questionnaireResponseItem.answer[0].item,
          context
        )
      }
      validateQuestionnaireResponseAnswers(
        questionnaireItem.item,
        questionnaireResponseItem.item,
        context
      )
    }
    return linkIdToValidationResultMap
  }

  /**
   * Checks that the [QuestionnaireResponse] is structurally consistent with the [Questionnaire].
   * - Each item in the [QuestionnaireResponse] must have a corresponding item in the
   * [Questionnaire] with the same `linkId` and `type`
   * - The order of items in the [QuestionnaireResponse] must be the same as the order of the items
   * in the [Questionnaire]
   * -
   * [Items nested under group](http://www.hl7.org/fhir/questionnaireresponse-definitions.html#QuestionnaireResponse.item.item)
   * and
   * [items nested under answer](http://www.hl7.org/fhir/questionnaireresponse-definitions.html#QuestionnaireResponse.item.answer.item)
   * should follow the same rules recursively
   *
   * Note that although all the items in the [Questionnaire] SHOULD be included in the
   * [QuestionnaireResponse], we do not throw an exception for missing items. This allows items that
   * are not enabled to be missing in the [QuestionnaireResponse].
   *
   * @throws IllegalArgumentException if `questionnaireResponse` is not for `questionnaire`
   * @throws IllegalArgumentException if there is no questionnaire item with the same `linkId` for a questionnaire response item
   * @throws IllegalArgumentException if the order of the questionnaire response items is not the same as that of the questionnaire items
   * @throws IllegalArgumentException if the type of a questionnaire response item does not match that of the corresponding questionnaire item
   * @throws IllegalArgumentException if a questionnaire response item is missing type
   * @throws
   *
   * See http://www.hl7.org/fhir/questionnaireresponse.html#link for more information.
   */
  fun checkQuestionnaireResponse(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    require(questionnaireResponse.questionnaire == questionnaire.id) {
      "Mismatch Questionnaire and QuestionnaireResponse. Questionnaire response is for Questionnaire ${questionnaire.id}."
    }
    checkQuestionnaireResponseItems(questionnaire.item, questionnaireResponse.item)
  }

  private fun checkQuestionnaireResponseItems(
    questionnaireItemList: List<Questionnaire.QuestionnaireItemComponent>,
    questionnaireResponseItemList: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>
  ) {
    val questionnaireItemIterator = questionnaireItemList.iterator()
    val questionnaireResponseInputItemIterator = questionnaireResponseItemList.iterator()

    while (questionnaireResponseInputItemIterator.hasNext()) {
      val questionnaireResponseItem = questionnaireResponseInputItemIterator.next()
      require(questionnaireItemIterator.hasNext()) {
        "Missing questionnaire item for questionnaire response item ${questionnaireResponseItem.linkId}"
      }
      var questionnaireItem = questionnaireItemIterator.next()
      while (questionnaireItem.id != questionnaireResponseItem.id) {
        require(questionnaireItemIterator.hasNext()) {
          "Missing questionnaire item for questionnaire response item ${questionnaireResponseItem.linkId}"
        }
        questionnaireItem = questionnaireItemIterator.next()
      }

      checkQuestionnaireResponseItem(questionnaireItem, questionnaireResponseItem)
    }
  }

  private fun checkQuestionnaireResponseItem(
    questionnaireItem: Questionnaire.QuestionnaireItemComponent,
    questionnaireResponseItem: QuestionnaireResponse.QuestionnaireResponseItemComponent,
  ) {
    val type = checkNotNull(questionnaireItem.type) { "Questionnaire item must have type" }


    if (questionnaireResponseItem.hasAnswer() && type != Questionnaire.QuestionnaireItemType.GROUP
    ) {
      require(questionnaireItem.repeats || questionnaireResponseItem.answer.size <= 1) {
        "Multiple answers in ${questionnaireResponseItem.linkId} and repeats false in " +
          "questionnaire item ${questionnaireItem.linkId}"
      }
      questionnaireResponseItem.answer.forEach {
        checkQuestionnaireResponseAnswerItem(questionnaireItem, it)
      }
    } else if (questionnaireResponseItem.hasItem()) {
      checkQuestionnaireResponseItems(questionnaireItem.item, questionnaireResponseItem.item)
    }
  }

  private fun checkQuestionnaireResponseAnswerItem(
    questionnaireItem: Questionnaire.QuestionnaireItemComponent,
    answerItem: QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent
  ) {
    if (answerItem.hasValue()) {
      when (questionnaireItem.type) {
        Questionnaire.QuestionnaireItemType.BOOLEAN,
        Questionnaire.QuestionnaireItemType.DECIMAL,
        Questionnaire.QuestionnaireItemType.INTEGER,
        Questionnaire.QuestionnaireItemType.DATE,
        Questionnaire.QuestionnaireItemType.DATETIME,
        Questionnaire.QuestionnaireItemType.TIME,
        Questionnaire.QuestionnaireItemType.STRING,
        Questionnaire.QuestionnaireItemType.URL ->
          require(answerItem.value.fhirType() == questionnaireItem.type.toCode()) {
            "Type mismatch for questionnaire item ${questionnaireItem.linkId}"
          }
        else -> Unit // Check type for primitives only
      }
    }
    // Nested items under answer http://www.hl7.org/fhir/questionnaireresponse-definitions.html#QuestionnaireResponse.item.answer.item
    checkQuestionnaireResponseItems(questionnaireItem.item, answerItem.item)
  }
}
