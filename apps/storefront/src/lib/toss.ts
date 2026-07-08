/**
 * Toss Payments v1 SDK lazy loader.
 *
 * VITE_TOSS_CLIENT_KEY가 설정된 경우에만 실제 결제창을 연다.
 * 결제 성공 후에는 successUrl(`/payment/success`)에서 백엔드 결제 승인을 요청한다.
 */

const SCRIPT_URL = 'https://js.tosspayments.com/v1/payment'

export interface TossRequestPaymentOptions {
  amount: number
  orderId: string
  orderName: string
  customerName?: string
  successUrl: string
  failUrl: string
}

interface TossPaymentsInstance {
  requestPayment(method: string, options: Record<string, unknown>): Promise<void>
}

type TossPaymentsFactory = (clientKey: string) => TossPaymentsInstance

declare global {
  interface Window {
    TossPayments?: TossPaymentsFactory
  }
}

let loadPromise: Promise<TossPaymentsFactory | null> | null = null

function loadScript(): Promise<TossPaymentsFactory | null> {
  if (loadPromise) return loadPromise

  loadPromise = new Promise<TossPaymentsFactory | null>((resolve) => {
    if (typeof window !== 'undefined' && window.TossPayments) {
      resolve(window.TossPayments)
      return
    }

    const script = document.createElement('script')
    script.src = SCRIPT_URL
    script.async = true
    script.onload = () => resolve(window.TossPayments ?? null)
    script.onerror = () => {
      loadPromise = null
      resolve(null)
    }
    document.head.appendChild(script)
  })

  return loadPromise
}

export function toTossMethod(methodName: string): string {
  switch (methodName) {
    case 'BANK_TRANSFER':
      return '계좌이체'
    case 'CARD':
    case 'EASY_PAY':
    default:
      return '카드'
  }
}

export async function requestTossPayment(
  clientKey: string,
  methodName: string,
  options: TossRequestPaymentOptions,
): Promise<void> {
  const factory = await loadScript()
  if (!factory) {
    throw new Error('TOSS_SDK_LOAD_FAILED')
  }

  const toss = factory(clientKey)
  await toss.requestPayment(toTossMethod(methodName), {
    amount: options.amount,
    orderId: options.orderId,
    orderName: options.orderName,
    customerName: options.customerName,
    successUrl: options.successUrl,
    failUrl: options.failUrl,
  })
}
