"""Campus building metadata used to recommend drop-off stops."""
from __future__ import annotations
from dataclasses import dataclass
from typing import Tuple

DEFAULT_BOARD_STOP_ID = "stop-incheon-univ-stn"

@dataclass(frozen=True)
class BuildingDef:
    id: str
    name: str
    lat: float
    lon: float
    stop_id: str
    aliases: Tuple[str, ...] = ()
    notes: str | None = None

BUILDINGS: Tuple[BuildingDef, ...] = (
    # ── 정문 / 캠퍼스 동측 상단 ─────────────────────────────────────────────
    BuildingDef(
        id="b1",
        name="1호관",
        lat=37.3782,
        lon=126.6352,
        stop_id="stop-main-gate",
        aliases=("1관", "1호관", "No1"),
        notes="정문 로터리 북측 (동문 라인)",
    ),
    BuildingDef(
        id="b2",
        name="2호관",
        lat=37.3780,
        lon=126.6355,
        stop_id="stop-main-gate",
        aliases=("2관", "2호관"),
        notes="정문 로터리 동측",
    ),
    BuildingDef(
        id="b3",
        name="3호관",
        lat=37.3775,
        lon=126.6356,
        stop_id="stop-main-gate",
        aliases=("3관", "3호관"),
        notes="정문 동측 블록",
    ),
    BuildingDef(
        id="b4",
        name="4호관",
        lat=37.3773,
        lon=126.6349,
        stop_id="stop-main-gate",
        aliases=("4관", "4호관"),
        notes="정문 남측 블록",
    ),
    BuildingDef(
        id="b13",
        name="13호관 (사회과학대)",
        lat=37.3770,
        lon=126.6362,
        stop_id="stop-main-gate",
        aliases=("13관", "13호관", "사회대", "사회과학대"),
        notes="정문 북동측 사회과학 계열",
    ),
    BuildingDef(
        id="b14",
        name="14호관 (글로벌정책대학)",
        lat=37.3768,
        lon=126.6365,
        stop_id="stop-main-gate",
        aliases=("14관", "14호관", "글로벌정책", "글로벌정책대"),
        notes="정문 북동측 글로벌정책 라인",
    ),
    BuildingDef(
        id="b15",
        name="15호관 (인문대학)",
        lat=37.3765,
        lon=126.6360,
        stop_id="stop-main-gate",
        aliases=("15관", "15호관", "인문대", "인문대학"),
        notes="인문사회계열 강의동",
    ),
    BuildingDef(
        id="b24",
        name="24호관",
        lat=37.3769,
        lon=126.6346,
        stop_id="stop-main-gate",
        aliases=("24관", "24호관"),
        notes="정문-본부 라인 우측",
    ),
    BuildingDef(
        id="b25",
        name="25호관",
        lat=37.3772,
        lon=126.6342,
        stop_id="stop-main-gate",
        aliases=("25관", "25호관"),
        notes="정문 진입부 동측 단지",
    ),
    # ── 공과대 앞 / 남측 중앙 ───────────────────────────────────────────────
    BuildingDef(
        id="b5",
        name="5호관 (자연과학대)",
        lat=37.3742,
        lon=126.6339,
        stop_id="stop-eng",
        aliases=("5관", "5호관", "자과대", "자연과학대"),
        notes="공대 정류장 바로 앞 자연대",
    ),
    BuildingDef(
        id="b6",
        name="6호관 (학산도서관)",
        lat=37.3745,
        lon=126.6341,
        stop_id="stop-eng",
        aliases=("6관", "6호관", "학산도서관", "도서관"),
        notes="학산도서관/중앙 학습공간",
    ),
    BuildingDef(
        id="b7",
        name="7호관 (정보기술대)",
        lat=37.3743,
        lon=126.6336,
        stop_id="stop-eng",
        aliases=("7관", "7호관", "정보기술대", "IT대"),
        notes="학산도서관 인접, IT/컴퓨터",
    ),
    BuildingDef(
        id="b8",
        name="8호관 (공과대학 본관)",
        lat=37.3740,
        lon=126.6346,
        stop_id="stop-eng",
        aliases=("8관", "8호관", "공대", "공과대"),
        notes="남측 도로 중앙 공대 클러스터",
    ),
    BuildingDef(
        id="b9",
        name="9호관 (공동실험실습관)",
        lat=37.3738,
        lon=126.6340,
        stop_id="stop-eng",
        aliases=("9관", "9호관", "공동실험실습관", "실습관"),
        notes="공동실험·실습동",
    ),
    BuildingDef(
        id="b10",
        name="10호관 (게스트하우스)",
        lat=37.3739,
        lon=126.6350,
        stop_id="stop-eng",
        aliases=("10관", "10호관", "게스트하우스", "게하"),
        notes="공대 라인 서측 게스트하우스",
    ),
    # ── 기숙사/체육 단지 (서남측) ───────────────────────────────────────────
    BuildingDef(
        id="b11",
        name="11호관 (생활원)",
        lat=37.3741,
        lon=126.6300,
        stop_id="stop-dorm",
        aliases=("11관", "11호관", "생활원11", "기숙사11"),
        notes="생활원/기숙사 라인",
    ),
    BuildingDef(
        id="b12",
        name="12호관 (생활원)",
        lat=37.3739,
        lon=126.6297,
        stop_id="stop-dorm",
        aliases=("12관", "12호관", "생활원12", "기숙사12"),
        notes="생활원 B동",
    ),
    BuildingDef(
        id="b16",
        name="16호관 (예·체대)",
        lat=37.3736,
        lon=126.6315,
        stop_id="stop-dorm",
        aliases=("16관", "16호관", "예체대", "예체능대"),
        notes="예체능 계열 강의동",
    ),
    BuildingDef(
        id="b17",
        name="17호관 (역사관 일대)",
        lat=37.3733,
        lon=126.6320,
        stop_id="stop-dorm",
        aliases=("17관", "17호관", "역사관"),
        notes="역사관/박물관 인근",
    ),
    BuildingDef(
        id="b18",
        name="18호관 (학생회관·복지동)",
        lat=37.3752,
        lon=126.6322,
        stop_id="stop-dorm",
        aliases=("18관", "18호관", "학생회관", "복지관"),
        notes="학생복지·동아리 시설 밀집",
    ),
    BuildingDef(
        id="b19",
        name="19호관 (융합과학기술대)",
        lat=37.3729,
        lon=126.6310,
        stop_id="stop-dorm",
        aliases=("19관", "19호관", "융합과기대"),
        notes="융합과학기술 계열",
    ),
    BuildingDef(
        id="b20",
        name="20호관 (스포츠센터)",
        lat=37.3738,
        lon=126.6290,
        stop_id="stop-dorm",
        aliases=("20관", "20호관", "스포츠센터"),
        notes="실내 스포츠센터",
    ),
    BuildingDef(
        id="b21",
        name="21호관 (체육관)",
        lat=37.3740,
        lon=126.6285,
        stop_id="stop-dorm",
        aliases=("21관", "21호관", "체육관"),
        notes="실내 체육/경기장",
    ),
    BuildingDef(
        id="b22",
        name="22호관",
        lat=37.3746,
        lon=126.6278,
        stop_id="stop-dorm",
        aliases=("22관", "22호관"),
        notes="체육·운동장 서측",
    ),
    BuildingDef(
        id="b23",
        name="23호관",
        lat=37.3750,
        lon=126.6280,
        stop_id="stop-dorm",
        aliases=("23관", "23호관"),
        notes="운동장/강당 주변",
    ),
)

__all__ = ["DEFAULT_BOARD_STOP_ID", "BuildingDef", "BUILDINGS"]
