import type { PageResult } from '@/types/auth'
import { get, patch, post, put } from '@/utils/request'

export interface FitnessScoreItem {
  itemCode: string
  itemName: string
  unit: string
  rawValue: number
  score: number
  weight: number
  weightedScore: number
  bonus: number
}

export interface FitnessScore {
  id: number
  userId: number
  studentName?: string
  studentNo?: string
  gender: 'MALE' | 'FEMALE'
  genderLabel: string
  groupType: 'UNIV_1_2' | 'UNIV_3_4'
  groupTypeLabel: string
  academicYear: string
  testDate: string
  heightCm: number
  weightKg: number
  bmi: number
  vitalCapacity?: number
  run50Seconds?: number
  sitReachCm?: number
  jumpCm?: number
  strengthCount?: number
  enduranceSeconds?: number
  standardScore: number
  bonusScore: number
  totalScore: number
  grade: string
  status: 'VALID' | 'INVALID'
  statusLabel: string
  auditOperator?: string
  auditTime?: string
  items: FitnessScoreItem[]
  missingItemCodes: string[]
}

export interface FitnessScorePayload {
  userId: number
  testDate: string
  heightCm: number
  weightKg: number
  vitalCapacity?: number | null
  run50Seconds?: number | null
  sitReachCm?: number | null
  jumpCm?: number | null
  strengthCount?: number | null
  enduranceSeconds?: number | null
  gender?: 'MALE' | 'FEMALE'
  groupType: 'UNIV_1_2' | 'UNIV_3_4'
  academicYear?: string | null
}

export interface FitnessScoreStudent {
  id: number
  name: string
  studentNo?: string
  gender?: number
  className?: string
}

export function previewFitnessScore(data: FitnessScorePayload) {
  return post<FitnessScore>('/fitness/scores/preview', data)
}

export function createFitnessScore(data: FitnessScorePayload) {
  return post<number>('/fitness/scores', data)
}

export function updateFitnessScore(id: number, data: FitnessScorePayload) {
  return put('/fitness/scores/' + id, data)
}

export function pageFitnessScores(params: {
  current: number
  size: number
  userId?: number
  testDate?: string
}) {
  return get<PageResult<FitnessScore>>('/fitness/scores', params)
}

export function myFitnessScores() {
  return get<FitnessScore[]>('/fitness/scores/my')
}

export function findFitnessScoreStudents(q = '') {
  return get<FitnessScoreStudent[]>('/fitness/scores/students', { q })
}

export function getFitnessScore(id: number) {
  return get<FitnessScore>('/fitness/scores/' + id)
}

export function changeFitnessScoreStatus(
  id: number,
  status: 'VALID' | 'INVALID',
  operator?: string
) {
  return patch<FitnessScore>('/fitness/scores/' + id + '/status', { status, operator })
}
