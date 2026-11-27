import type { PageResult } from '@/types/auth'
import { get, post, put } from '@/utils/request'

export interface FitnessInput {
  userId: number
  testDate: string
  heightCm: number
  weightKg: number
  vitalCapacity: number
  run50Seconds: number
  jumpCm: number
}

export interface FitnessRecord extends FitnessInput {
  id: number
  studentName?: string
  studentNo?: string
}

export interface FitnessStudent {
  id: number
  name: string
  studentNo?: string
}

export interface FitnessClusters {
  features: string[]
  excludedCount: number
  points: Array<{ bmi: number; run50Seconds: number; cluster: number }>
  analysis: {
    centers: number[][]
    counts: number[]
    iterations: number
    sse: number
  }
}

export function listFitness(params: {
  current: number
  size: number
  userId?: number
  testDate?: string
}) {
  return get<PageResult<FitnessRecord>>('/fitness', params)
}

export function findFitnessStudents(q: string) {
  return get<FitnessStudent[]>('/fitness/students', { q })
}

export function saveFitness(input: FitnessInput, id?: number) {
  return id === undefined ? post('/fitness', input) : put('/fitness/' + id, input)
}

export function analyzeFitness(testDate: string, k: number) {
  return get<FitnessClusters>('/fitness/clusters', { testDate, k })
}
