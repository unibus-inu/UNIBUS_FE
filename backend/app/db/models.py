from sqlalchemy import String, Integer, Float, Text, ForeignKey, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.db.base import Base

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