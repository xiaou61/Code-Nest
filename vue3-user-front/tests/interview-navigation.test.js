import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { buildInterviewQuestionSetRoute } from '../src/utils/interview-navigation.js'

const projectRoot = resolve(import.meta.dirname, '..')

test('buildInterviewQuestionSetRoute should include set id and question id for search results', () => {
  const route = buildInterviewQuestionSetRoute({
    id: 'q-456',
    originalQuestion: {
      id: 456,
      questionSetId: 123
    }
  })

  assert.equal(route, '/interview/questions/123/456')
})

test('interview index should route question set clicks through the shared route builder', () => {
  const source = readFileSync(resolve(projectRoot, 'src/views/interview/Index.vue'), 'utf8')

  assert.match(source, /buildInterviewQuestionSetRoute/)
  assert.match(source, /router\.push\(buildInterviewQuestionSetRoute\(questionSet\)\)/)
})
