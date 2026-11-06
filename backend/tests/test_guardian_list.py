"""피보호자가 연결된 보호자 목록을 조회하는 API (연결 코드 화면에서 연결 완료 확인용)."""

import pytest
from fastapi import HTTPException
from sqlmodel import Session, SQLModel, create_engine

from app.guardian.router import get_guardians
from app.models import Device, GuardianLink


@pytest.fixture
def session():
    engine = create_engine("sqlite://")
    SQLModel.metadata.create_all(engine)
    with Session(engine) as s:
        s.add(Device(device_id="p1", role="protected"))
        s.add(Device(device_id="g1", role="guardian"))
        s.add(Device(device_id="g2", role="guardian"))
        s.commit()
        yield s


def test_empty_before_connect(session):
    r = get_guardians("p1", session)
    assert r.protected_device_id == "p1" and r.guardians == []


def test_lists_only_active_guardians(session):
    session.add(GuardianLink(protected_device_id="p1", guardian_device_id="g1", is_active=True))
    session.add(GuardianLink(protected_device_id="p1", guardian_device_id="g2", is_active=False))
    session.commit()
    r = get_guardians("p1", session)
    assert [g.guardian_device_id for g in r.guardians] == ["g1"]


def test_unknown_or_guardian_device_is_404(session):
    for device_id in ("nope", "g1"):
        with pytest.raises(HTTPException) as e:
            get_guardians(device_id, session)
        assert e.value.status_code == 404
