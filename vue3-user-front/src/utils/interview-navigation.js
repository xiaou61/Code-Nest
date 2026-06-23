export function buildInterviewQuestionSetRoute(questionSet) {
  const originalQuestion = questionSet?.originalQuestion

  if (typeof questionSet?.id === 'string' && questionSet.id.startsWith('q-') && originalQuestion) {
    const questionSetId = originalQuestion.questionSetId ?? originalQuestion.setId
    const questionId = originalQuestion.id

    if (questionSetId && questionId) {
      return `/interview/questions/${questionSetId}/${questionId}`
    }

    return '/interview'
  }

  return `/interview/question-sets/${questionSet.id}`
}
