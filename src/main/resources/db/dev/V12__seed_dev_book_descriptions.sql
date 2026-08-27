-- DEV ONLY: descriptions for the deterministic demo catalog.
-- This migration enriches existing development fixtures without changing
-- production migrations or previously executed versions.

UPDATE books
SET description = CASE isbn
    WHEN '9780451524935' THEN 'Em uma sociedade totalitária onde o Estado controla a informação, Winston Smith trabalha alterando registros históricos. Ao questionar a realidade imposta pelo Partido, ele começa a buscar uma verdade que pode lhe custar tudo.'
    WHEN '9780141439518' THEN 'Elizabeth Bennet enfrenta as expectativas sociais de sua época enquanto conhece o reservado e orgulhoso Mr. Darcy. A história explora relações, preconceitos, classe social e as consequências de julgar alguém pelas primeiras impressões.'
    WHEN '9780743273565' THEN 'No verão de 1922, Nick Carraway observa a vida extravagante de seu vizinho Jay Gatsby e seu círculo social em Long Island. Entre festas, riqueza e segredos, Gatsby tenta recuperar um amor do passado.'
    WHEN '9780316769488' THEN 'Holden Caulfield narra alguns dias turbulentos após ser expulso de sua escola. Entre encontros em Nova York e reflexões sobre a vida adulta, ele procura sentido, autenticidade e um lugar onde se sinta pertencente.'
    ELSE description
END
WHERE isbn IN (
    '9780451524935',
    '9780141439518',
    '9780743273565',
    '9780316769488'
);
