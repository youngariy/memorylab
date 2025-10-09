import { motion } from 'framer-motion';
import { useInView } from 'framer-motion';
import { useRef } from 'react';
import styles from './Cards.module.css';
import info1 from '@/assets/info1.png';
import info2 from '@/assets/info2.png';
import info3 from '@/assets/info3.png';

const cards = [
  {
    id: 'card1',
    title: '누구나',
    description:
      '누구나 쉽게 사용할 수 있습니다.\n지금 휴대폰을 켜 동영상을 촬영하세\n요',
    mobileDescription:
      '누구나 쉽게 사용할 수 있습니다.\n지금 휴대폰을 켜\n동영상을 촬영하세요',
    backgroundImage: info1,
  },
  {
    id: 'card2',
    title: '손쉽게',
    description:
      '동영상을 업로드만 하세요\n추억현상소의 AI가 메타버스로 만들어 드립니다',
    mobileDescription:
      '동영상을 업로드만 하세요\n추억현상소의 AI가\n메타버스로 만들어 드립니다',
    backgroundImage: info2,
  },
  {
    id: 'card3',
    title: '강렬히',
    description:
      '기억과 사진에 의존한 추억의 공간\n이제는 메타버스로 경험하세요',
    mobileDescription:
      '기억과 사진에\n의존한 추억의 공간\n이제는 메타버스로 경험하세요',
    backgroundImage: info3,
  },
];

function Cards() {
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-100px' });

  return (
    <section className={styles.cards} ref={ref}>
      <div className={styles.grid}>
        {cards.map((card, index) => {
          return (
            <motion.div
              key={card.id}
              className={styles.card}
              initial={{ y: 60, opacity: 0 }}
              animate={isInView ? { y: 0, opacity: 1 } : { y: 60, opacity: 0 }}
              transition={{ duration: 0.6, delay: 0.2 + index * 0.1 }}
              whileHover={{ y: -10, scale: 1.02 }}
            >
              <div
                className={styles.cardBackground}
                style={{
                  backgroundImage: `url(${card.backgroundImage})`,
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                }}
              />
              <div className={styles.cardOverlay} />

              <div className={styles.cardContent}>
                <h3 className={styles.cardTitle}>{card.title}</h3>

                {/* PC용 description */}
                <p className={styles.cardDescription}>
                  {card.description.split('\n').map((line, i) => (
                    <span key={i}>
                      {line}
                      {i < card.description.split('\n').length - 1 && <br />}
                    </span>
                  ))}
                </p>

                {/* 모바일용 description */}
                <p className={styles.cardDescriptionMobile}>
                  {card.mobileDescription.split('\n').map((line, i) => (
                    <span key={i}>
                      {line}
                      {i < card.mobileDescription.split('\n').length - 1 && (
                        <br />
                      )}
                    </span>
                  ))}
                </p>
              </div>
            </motion.div>
          );
        })}
      </div>
    </section>
  );
}

export default Cards;
