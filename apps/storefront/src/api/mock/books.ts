import type { BookResponse, BookReviewResponse, CategoryResponse } from '../contracts'

export const MOCK_CATEGORIES: CategoryResponse[] = [
  { categoryId: 1, categoryName: '소설' },
  { categoryId: 2, categoryName: '인문' },
  { categoryId: 3, categoryName: '과학' },
  { categoryId: 4, categoryName: '예술' },
  { categoryId: 5, categoryName: '에세이' },
]

function book(
  id: number,
  title: string,
  author: string,
  price: number,
  categoryId: number,
  publisher: string,
  pubDate: string,
  avgRating: number,
  reviewCount: number,
  content: string,
): BookResponse {
  const category = MOCK_CATEGORIES.find((c) => c.categoryId === categoryId) ?? MOCK_CATEGORIES[0]
  return {
    id,
    title,
    author,
    isbn: `979-11-0000-${String(id).padStart(4, '0')}`,
    price,
    imageUrl: null,
    categories: [category],
    tags: null,
    content,
    publisher,
    pubDate,
    avgRating,
    reviewCount,
    aiSummary: null,
    aiReviewSummary: null,
    categoryId: category.categoryId,
    parentId: null,
  }
}

export const MOCK_BOOKS: BookResponse[] = [
  book(1, '한밤의 도서관', '김서연', 16800, 1, '하이파이브북스', '2026-05-12', 4.8, 214, '자정이 되면 문을 여는 도서관, 그곳에서 다시 쓰는 인생의 챕터.'),
  book(2, '별을 세는 법', '이준호', 15300, 3, '코스모스', '2026-04-02', 4.6, 158, '천문학자가 들려주는 우주와 고독에 관한 열두 밤의 강의.'),
  book(3, '고요한 아침의 감각', '박다인', 14500, 5, '모먼트', '2026-06-01', 4.7, 96, '하루의 시작을 바꾸는 아주 작은 리추얼에 대하여.'),
  book(4, '유리 정원', '한지우', 17800, 1, '하이파이브북스', '2026-03-18', 4.5, 301, '온실 속에서 자란 두 사람이 바깥의 계절을 견디는 방식.'),
  book(5, '생각의 건축', '정민규', 19800, 2, '아크북', '2026-02-25', 4.9, 187, '흔들리지 않는 사고의 구조를 세우는 아홉 개의 기둥.'),
  book(6, '바다가 기억하는 것들', '최유라', 16200, 1, '파도', '2026-05-30', 4.4, 122, '해녀 삼대의 목소리로 기록한 물속의 시간들.'),
  book(7, '숫자 없는 수학', '오태양', 18500, 3, '코스모스', '2026-01-15', 4.6, 143, '수식 한 줄 없이 읽는 수학적 사고의 아름다움.'),
  book(8, '색을 듣는 사람', '윤채원', 21000, 4, '팔레트', '2026-04-20', 4.8, 89, '공감각 화가가 안내하는 색채와 소리의 미술관.'),
  book(9, '느리게 걷는 연습', '서하늘', 13800, 5, '모먼트', '2026-06-15', 4.3, 74, '속도를 줄일 때 비로소 보이는 풍경에 관한 에세이.'),
  book(10, '미래에서 온 편지', '강도윤', 15900, 1, '하이파이브북스', '2026-02-09', 4.7, 265, '30년 뒤의 나에게서 도착한 열두 통의 편지.'),
  book(11, '식탁 위의 인문학', '임소정', 17300, 2, '아크북', '2026-03-05', 4.5, 118, '한 끼의 식사에 담긴 문명과 취향의 역사.'),
  book(12, '빛의 문법', '노아름', 22500, 4, '팔레트', '2026-05-02', 4.9, 152, '사진가가 말하는 빛을 읽고 쓰는 법.'),
]

export const MOCK_REVIEWS: BookReviewResponse[] = [
  {
    reviewId: 1,
    memberId: 11,
    loginId: 'bookworm_92',
    content: '올해 읽은 책 중 단연 최고. 마지막 장을 덮고 한참을 멍하니 있었다.',
    rating: 5,
    createdAt: '2026-06-20T10:12:00',
    reviewImages: null,
    likeCount: 24,
    isLiked: false,
  },
  {
    reviewId: 2,
    memberId: 12,
    loginId: 'quiet_reader',
    content: '문장이 아름답다. 밑줄 긋느라 진도가 안 나가는 책.',
    rating: 4,
    createdAt: '2026-06-18T22:40:00',
    reviewImages: null,
    likeCount: 11,
    isLiked: false,
  },
  {
    reviewId: 3,
    memberId: 13,
    loginId: 'midnight_lib',
    content: '선물용으로 한 권 더 샀습니다. 표지도 내용도 만족.',
    rating: 5,
    createdAt: '2026-06-10T08:03:00',
    reviewImages: null,
    likeCount: 7,
    isLiked: false,
  },
]
