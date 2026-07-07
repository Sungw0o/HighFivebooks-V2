/**
 * Daum 우편번호 서비스 lazy load 유틸리티.
 * 외부 스크립트이므로 동적 로드하고, 실패 시 수기 입력 fallback을 유지한다.
 *
 * @see https://postcode.map.daum.net/guide
 */

// eslint-disable-next-line @typescript-eslint/no-namespace
declare namespace daum {
  class Postcode {
    constructor(opts: {
      oncomplete: (data: DaumPostcodeResult) => void
      onclose?: () => void
    })
    open(): void
  }
}

export interface DaumPostcodeResult {
  /** 우편번호 */
  zonecode: string
  /** 도로명 주소 */
  roadAddress: string
  /** 지번 주소 */
  jibunAddress: string
  /** 건물명 */
  buildingName: string
  /** 자동 생성 주소 */
  autoRoadAddress: string
  /** 자동 생성 지번 주소 */
  autoJibunAddress: string
}

const SCRIPT_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
let loadPromise: Promise<boolean> | null = null

/** 스크립트를 한 번만 로드한다. 성공 시 true, 실패 시 false */
function loadScript(): Promise<boolean> {
  if (loadPromise) return loadPromise

  loadPromise = new Promise<boolean>((resolve) => {
    if (typeof window !== 'undefined' && 'daum' in window) {
      resolve(true)
      return
    }

    const script = document.createElement('script')
    script.src = SCRIPT_URL
    script.async = true
    script.onload = () => resolve(true)
    script.onerror = () => {
      loadPromise = null // 실패 시 재시도 가능
      resolve(false)
    }
    document.head.appendChild(script)
  })

  return loadPromise
}

/**
 * Daum 우편번호 팝업을 열고, 선택 결과를 콜백으로 전달한다.
 * 스크립트 로드 실패 시 false를 반환하여 수기 입력 fallback을 유지한다.
 */
export async function openDaumPostcode(
  onComplete: (result: DaumPostcodeResult) => void,
): Promise<boolean> {
  const loaded = await loadScript()
  if (!loaded) return false

  new daum.Postcode({ oncomplete: onComplete }).open()
  return true
}
