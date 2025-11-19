from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.db.base import Base  # 공용 Declarative Base

class RideSurvey(Base):
    __tablename__ = "ride_survey"
    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(64), nullable=True)          # 익명/가명 가능
    vehicle_id = Column(String(64), nullable=False)
    board_stop = Column(String(128), nullable=False)
    board_time = Column(DateTime(timezone=True), nullable=False)      # UTC 저장 권장
    class_building = Column(String(32), nullable=False)  # 예: "N"
    class_room = Column(String(32), nullable=False)      # 예: "401"
    class_start_time = Column(DateTime(timezone=True), nullable=False)
    arrival_time = Column(DateTime(timezone=True), nullable=False)
    travel_time_min = Column(Integer, nullable=False)
    early_min = Column(Integer, nullable=False, default=0)   # 수업 기준 ‘얼마나 일찍’
    late_min  = Column(Integer, nullable=False, default=0)   # 수업 기준 ‘얼마나 지각’
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

class Route(Base):
    __tablename__ = "routes"
    id: Mapped[str] = mapped_column(String, primary_key=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    polyline: Mapped[str] = mapped_column(Text, nullable=False)  # GeoJSON/encoded polyline

    stops: Mapped[list["Stop"]] = relationship(back_populates="route")

class Stop(Base):
    __tablename__ = "stops"
    id: Mapped[str] = mapped_column(String, primary_key=True)
    route_id: Mapped[str] = mapped_column(ForeignKey("routes.id"), index=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    lat: Mapped[float] = mapped_column(Float, nullable=False)
    lon: Mapped[float] = mapped_column(Float, nullable=False)
    seq: Mapped[int] = mapped_column(Integer, nullable=False)
    route: Mapped["Route"] = relationship(back_populates="stops")

    __table_args__ = (UniqueConstraint("route_id","seq", name="uq_stop_seq"),)

class Vehicle(Base):
    __tablename__ = "vehicles"
    id: Mapped[str] = mapped_column(String, primary_key=True)
    route_id: Mapped[str] = mapped_column(ForeignKey("routes.id"))
    plate: Mapped[str | None] = mapped_column(String, nullable=True)
    device_secret: Mapped[str] = mapped_column(String, nullable=False)
    last_seen_ts: Mapped[int | None] = mapped_column(Integer, nullable=True)

class Position(Base):
    __tablename__ = "positions"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    vehicle_id: Mapped[str] = mapped_column(ForeignKey("vehicles.id"), index=True)
    ts: Mapped[int] = mapped_column(Integer, index=True)
    lat: Mapped[float] = mapped_column(Float)
    lon: Mapped[float] = mapped_column(Float)
    speed_mps: Mapped[float | None] = mapped_column(Float)
    heading: Mapped[float | None] = mapped_column(Float)
    hdop: Mapped[float | None] = mapped_column(Float)
    src: Mapped[str | None] = mapped_column(String, default="gps")

VehiclePosition = Position


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    email = Column(String(255), unique=True, nullable=False, index=True)
    full_name = Column(String(255), nullable=True)
    password_hash = Column(String(255), nullable=False)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    tokens: Mapped[list["AuthToken"]] = relationship(back_populates="user", cascade="all, delete-orphan")


class AuthToken(Base):
    __tablename__ = "auth_tokens"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    token = Column(String(128), nullable=False, unique=True, index=True)
    issued_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)

    user: Mapped["User"] = relationship(back_populates="tokens")
